"""
Backup / reference MongoDB -> ClickHouse ETL Pipeline
Updated for Airflow 3.x compatibility with lazy client initialization.
Uses os.getenv (not Airflow Variables) for all config.
"""

from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator
from datetime import datetime, timedelta
import logging
import os
import pandas as pd
from pymongo import MongoClient
import clickhouse_connect
from typing import Dict
from airflow.exceptions import AirflowException
from contextlib import contextmanager

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)


def _get_env(key: str, default: str = "") -> str:
    """Read environment variable."""
    return os.getenv(key, default)


@contextmanager
def get_mongo_connection():
    mongo_uri = _get_env("MONGO_URI")
    if not mongo_uri:
        raise AirflowException("MONGO_URI environment variable is not set")
    client = MongoClient(mongo_uri, serverSelectionTimeoutMS=10000, tlsAllowInvalidCertificates=True)
    db = client[_get_env("MONGO_DB_NAME", "goandstudydb")]
    try:
        yield client, db
    finally:
        client.close()


def get_ch_client():
    """Create a fresh ClickHouse client each call (safe for multiprocessing)."""
    return clickhouse_connect.get_client(
        host=_get_env("CLICKHOUSE_HOST", "clickhouse"),
        port=int(_get_env("CLICKHOUSE_PORT", "8123")),
        username=_get_env("CLICKHOUSE_USER", "default"),
        password=_get_env("CLICKHOUSE_PASSWORD", ""),
        database=_get_env("CLICKHOUSE_DB", "library_dw"),
        settings={"max_execution_time": 300},
    )


def get_schema(table: str) -> list:
    ch = get_ch_client()
    try:
        return [r[0] for r in ch.query(f"DESCRIBE {table}").result_rows]
    finally:
        ch.close()


def safe_nulls(df: pd.DataFrame) -> pd.DataFrame:
    for col in df.select_dtypes(include=["object"]).columns:
        if df[col].apply(lambda x: isinstance(x, list)).any():
            df[col] = df[col].apply(lambda x: x if isinstance(x, list) else [])
        else:
            df[col] = df[col].fillna("")
    numeric_cols = df.select_dtypes(include=["number"]).columns
    df[numeric_cols] = df[numeric_cols].fillna(0)
    return df


def make_time_id(df: pd.DataFrame, date_field: str) -> pd.DataFrame:
    if date_field in df.columns:
        df["time_id"] = (
            pd.to_datetime(df[date_field], errors="coerce")
            .dt.strftime("%Y%m%d")
            .astype("Int64")
            .fillna(0)
        )
    return df


def _insert_batch(table, batch, mapping, schema):
    df = pd.DataFrame(batch)
    df = safe_nulls(df)
    id_col = "_id" if "_id" in df.columns else "id"
    df["_id_str"] = df[id_col].astype(str)
    df = df.rename(columns=mapping)
    for col in schema:
        if col not in df.columns:
            df[col] = pd.NA
    df_final = df[schema]
    ch = get_ch_client()
    try:
        ch.insert_df(table, df_final)
    finally:
        ch.close()
    return len(df_final)


def _insert_fact_batch(table, batch, mapping, schema, date_field):
    df = pd.DataFrame(batch)
    df = safe_nulls(df)
    df = make_time_id(df, date_field)
    id_col = "_id" if "_id" in df.columns else "id"
    df["_id_str"] = df[id_col].astype(str)
    df = df.rename(columns=mapping)
    for col in schema:
        if col not in df.columns:
            df[col] = pd.NA
    df_final = df[schema]
    ch = get_ch_client()
    try:
        ch.insert_df(table, df_final)
    finally:
        ch.close()
    return len(df_final)


def sync_dimension(collection: str, table: str, mapping: Dict):
    try:
        logger.info(f"=== START SYNC: {collection} -> {table} ===")
        schema = get_schema(table)
        logger.info(f"{table}: Target schema columns: {len(schema)}")

        date_field_map = {
            "Member": "timeCreated",
            "Book": "createdAt",
            "Author": "timeCreated",
            "Category": "timeCreated",
        }
        date_field = date_field_map.get(collection, "createdAt")

        query = {}
        logger.info(f"{table}: FULL LOAD MODE")

        row_count = 0
        mongo_fetched = 0

        with get_mongo_connection() as (_, db):
            coll = db[collection]
            total = coll.count_documents({})
            matching = coll.count_documents(query)
            logger.info(f"{table}: MongoDB {collection} total={total}, matching={matching}")

            cursor = coll.find(query, sort=[(date_field, 1), ("_id", 1)]).batch_size(2000)
            batch = []
            for doc in cursor:
                batch.append(doc)
                mongo_fetched += 1
                if len(batch) >= 2000:
                    row_count += _insert_batch(table, batch, mapping, schema)
                    logger.info(f"{table}: batch insert (total: {row_count})")
                    batch = []

            if batch:
                row_count += _insert_batch(table, batch, mapping, schema)
                logger.info(f"{table}: final batch {len(batch)} rows")

        logger.info(f"{table}: COMPLETED - fetched={mongo_fetched}, inserted={row_count}")
        return row_count

    except Exception as e:
        logger.error(f"{collection} sync FAILED: {e}")
        raise AirflowException(f"Dimension sync failed for {collection}")


def sync_fact(collection: str, table: str, mapping: Dict, date_field: str):
    try:
        logger.info(f"=== START FACT SYNC: {collection} -> {table} ===")
        schema = get_schema(table)

        query = {}
        row_count = 0
        mongo_fetched = 0

        with get_mongo_connection() as (_, db):
            coll = db[collection]
            cursor = coll.find(query, sort=[(date_field, 1), ("_id", 1)]).batch_size(2000)
            batch = []
            for doc in cursor:
                batch.append(doc)
                mongo_fetched += 1
                if len(batch) >= 2000:
                    row_count += _insert_fact_batch(table, batch, mapping, schema, date_field)
                    batch = []

            if batch:
                row_count += _insert_fact_batch(table, batch, mapping, schema, date_field)

        logger.info(f"{table}: COMPLETED - fetched={mongo_fetched}, inserted={row_count}")
        return row_count

    except Exception as e:
        logger.error(f"fact sync {collection} failed: {e}")
        raise AirflowException(f"Fact sync failed for {collection}")


def sync_bridge():
    try:
        ch = get_ch_client()
        try:
            ch.command("TRUNCATE TABLE bridge_book_author")
        finally:
            ch.close()

        schema = get_schema("bridge_book_author")
        row_count = 0
        with get_mongo_connection() as (_, db):
            books = db["Book"].find({}, {"id": 1, "authorIds": 1}).batch_size(2000)
            rows = []
            for book in books:
                bid = str(book["id"])
                for aid in book.get("authorIds", []):
                    rows.append({"book_id": bid, "author_id": str(aid)})

            df = pd.DataFrame(rows)
            df = safe_nulls(df)
            for col in schema:
                if col not in df.columns:
                    df[col] = pd.NA
            df_final = df[schema]
            ch = get_ch_client()
            try:
                ch.insert_df("bridge_book_author", df_final)
            finally:
                ch.close()
            row_count = len(df_final)
            logger.info(f"bridge_book_author: TRUNCATE + INSERT {row_count} rows")
    except Exception as e:
        logger.error(f"bridge sync failed: {e}")
        raise AirflowException("Bridge sync failed")


def _sync_dim_category_impl():
    try:
        schema = get_schema("dim_category")
        row_count = 0
        with get_mongo_connection() as (_, db):
            pipeline = [
                {"$match": {"categoryId": {"$ne": None}}},
                {"$group": {"_id": "$categoryId"}},
                {"$project": {"category_id": "$_id", "name": "$_id"}},
            ]
            data = list(db["BookCategory"].aggregate(pipeline))
            df = pd.DataFrame(data)
            df = safe_nulls(df)
            for col in schema:
                if col not in df.columns:
                    df[col] = pd.NA
            df_final = df[schema]
            ch = get_ch_client()
            try:
                ch.insert_df("dim_category", df_final)
            finally:
                ch.close()
            row_count = len(df_final)
            logger.info(f"dim_category: {row_count} unique categories")
    except Exception as e:
        logger.error(f"dim_category failed: {e}")
        raise AirflowException("dim_category failed")


def data_quality_check():
    ch = get_ch_client()
    try:
        checks = [
            ("dim_user", ch.query("SELECT count() FROM dim_user").result_rows[0][0]),
            ("dim_book", ch.query("SELECT count() FROM dim_book").result_rows[0][0]),
            ("fact_loan", ch.query("SELECT count() FROM fact_loan").result_rows[0][0]),
            ("fact_events", ch.query("SELECT count() FROM fact_events").result_rows[0][0]),
            ("bridge_book_author", ch.query("SELECT count() FROM bridge_book_author").result_rows[0][0]),
        ]
        for table_name, count in checks:
            logger.info(f"DQ: {table_name} = {count:,} rows")

        if not all(count > 0 for _, count in checks):
            raise AirflowException("Data quality check failed - some tables are empty")
        logger.info("DQ CHECK PASSED")
        return checks
    finally:
        ch.close()


def sync_dim_user(**context):
    mapping = {
        "_id_str": "user_id",
        "name": "name",
        "email": "email",
        "phone": "phone",
        "role": "role",
        "membershipType": "membership_type",
        "preferredGenres": "preferred_genres",
        "totalLoans": "total_loans",
        "activeLoanCount": "active_loans",
        "timeCreated": "created_at",
        "updatedAt": "updated_at",
    }
    return sync_dimension("Member", "dim_user", mapping)


def sync_dim_book(**context):
    mapping = {
        "_id_str": "book_id",
        "isbn": "isbn",
        "title": "title",
        "authorIds": "author_ids",
        "categoryIds": "category_ids",
        "description": "description",
        "averageRating": "average_rating",
        "totalRatings": "total_ratings",
        "totalIssues": "total_issues",
        "availableCopies": "available_copies",
        "createdAt": "created_at",
        "updatedAt": "updated_at",
    }
    return sync_dimension("Book", "dim_book", mapping)


def sync_dim_author(**context):
    mapping = {"_id_str": "author_id", "name": "name"}
    return sync_dimension("Author", "dim_author", mapping)


def sync_dim_category(**context):
    return _sync_dim_category_impl()


def sync_bridge_book_author(**context):
    return sync_bridge()


def sync_fact_loan(**context):
    mapping = {
        "_id_str": "loan_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "bookIsbn": "isbn",
        "status": "status",
        "issuedAt": "issued_at",
        "dueDate": "due_date",
        "returnedAt": "returned_at",
        "renewalCount": "renewal_count",
        "fineAmount": "fine_amount",
        "overdueDays": "overdue_days",
        "createdAt": "created_at",
    }
    return sync_fact("Loan", "fact_loan", mapping, date_field="createdAt")


def sync_fact_payment(**context):
    mapping = {
        "_id_str": "payment_id",
        "memberId": "user_id",
        "loanId": "loan_id",
        "amount": "amount",
        "paidAt": "payment_time",
    }
    return sync_fact("FineRecord", "fact_payment", mapping, date_field="paidAt")


def sync_fact_events(**context):
    mapping = {
        "_id_str": "event_id",
        "loanId": "loan_id",
        "eventType": "event_type",
        "timestamp": "timestamp",
        "staffId": "triggered_by",
    }
    return sync_fact("LoanEvent", "fact_events", mapping, date_field="timestamp")


def sync_fact_ratings(**context):
    mapping = {
        "_id_str": "review_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "rating": "rating",
        "comment": "review_text",
        "createdAt": "created_at",
    }
    return sync_fact("BookReview", "fact_ratings", mapping, date_field="createdAt")


def sync_fact_wishlist(**context):
    mapping = {
        "_id_str": "wishlist_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "addedAt": "added_at",
    }
    return sync_fact("Wishlist", "fact_wishlist", mapping, date_field="addedAt")


def sync_fact_search(**context):
    mapping = {
        "_id_str": "search_id",
        "memberId": "user_id",
        "query": "query",
        "createdAt": "created_at",
    }
    return sync_fact("SearchLog", "fact_search", mapping, date_field="createdAt")


def run_dq_check(**context):
    return data_quality_check()


default_args = {
    "owner": "dataeng",
    "depends_on_past": False,
    "retries": 3,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(hours=2),
}

with DAG(
    "mongodb_clickhouse_library_etl_backup",
    default_args=default_args,
    description="Production MongoDB->ClickHouse Library ETL Pipeline (Backup)",
    schedule="@daily",
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["etl", "prod", "library", "mongodb", "clickhouse", "backup"],
    max_active_runs=1,
) as dag:

    dim_user = PythonOperator(task_id="dim_user", python_callable=sync_dim_user)
    dim_book = PythonOperator(task_id="dim_book", python_callable=sync_dim_book)
    dim_author = PythonOperator(task_id="dim_author", python_callable=sync_dim_author)
    dim_category = PythonOperator(task_id="dim_category", python_callable=sync_dim_category)
    bridge_book_author = PythonOperator(task_id="bridge_book_author", python_callable=sync_bridge_book_author)

    fact_loan = PythonOperator(task_id="fact_loan", python_callable=sync_fact_loan)
    fact_payment = PythonOperator(task_id="fact_payment", python_callable=sync_fact_payment)
    fact_events = PythonOperator(task_id="fact_events", python_callable=sync_fact_events)
    fact_ratings = PythonOperator(task_id="fact_ratings", python_callable=sync_fact_ratings)
    fact_wishlist = PythonOperator(task_id="fact_wishlist", python_callable=sync_fact_wishlist)
    fact_search = PythonOperator(task_id="fact_search", python_callable=sync_fact_search)

    dq_check = PythonOperator(task_id="data_quality_check", python_callable=run_dq_check)

    (
        [dim_user, dim_book, dim_author, dim_category]
        >> bridge_book_author
        >> [fact_loan, fact_payment, fact_events, fact_ratings, fact_wishlist, fact_search]
        >> dq_check
    )

