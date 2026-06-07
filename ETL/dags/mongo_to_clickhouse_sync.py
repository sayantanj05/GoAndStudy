"""
MongoDB -> ClickHouse ETL Pipeline (FIXED v3)
- Lazy client initialization (no connections at DAG parse time)
- Airflow 3.x compatible imports
- Uses os.getenv with Docker-network defaults
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

# ============================================================================
# CONFIGURATION
# ============================================================================

FULL_LOAD = True  # Set True for initial full load, False for incremental CDC

logger = logging.getLogger(__name__)


def _get_env(key: str, default: str = "") -> str:
    """Read env var; os.getenv is safest during both parse and runtime."""
    return os.getenv(key, default)


# ============================================================================
# CONNECTION HELPERS — LAZY (called only inside task runtime)
# ============================================================================

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


# ============================================================================
# DATA TRANSFORMATION
# ============================================================================

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


# ============================================================================
# SYNC FUNCTIONS
# ============================================================================


def sync_dimension(collection: str, table: str, mapping: Dict):
    """Sync a dimension table from MongoDB to ClickHouse."""
    try:
        logger.info(f"=== START SYNC: {collection} -> {table} ===")
        schema = get_schema(table)
        logger.info(f"{table}: Target schema columns: {len(schema)}")

        date_field_map = {
            "members": "timeCreated",
            "books": "createdAt",
            "authors": "timeCreated",
            "categories": "timeCreated",
        }
        date_field = date_field_map.get(collection, "createdAt")

        if FULL_LOAD:
            query = {}
            logger.info(f"{table}: FULL LOAD MODE")
        else:
            watermark = _get_env(f"{table}_wm", "1970-01-01")
            query = {
                "$or": [
                    {"updatedAt": {"$gt": pd.to_datetime(watermark)}},
                    {date_field: {"$gt": pd.to_datetime(watermark)}},
                ]
            }
            logger.info(f"{table}: CDC MODE - watermark={watermark}")

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


def sync_fact(collection: str, table: str, mapping: Dict, date_field: str):
    """Sync a fact table from MongoDB to ClickHouse."""
    try:
        logger.info(f"=== START FACT SYNC: {collection} -> {table} ===")
        schema = get_schema(table)

        if FULL_LOAD:
            query = {}
        else:
            watermark = _get_env(f"{table}_wm", "1970-01-01")
            query = {
                "$or": [
                    {"updatedAt": {"$gt": pd.to_datetime(watermark)}},
                    {date_field: {"$gt": pd.to_datetime(watermark)}},
                ]
            }

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


# ============================================================================
# TASK CALLABLES
# ============================================================================


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
    return sync_dimension("members", "dim_user", mapping)


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
    return sync_dimension("books", "dim_book", mapping)


def sync_dim_author(**context):
    mapping = {"_id_str": "author_id", "name": "name"}
    return sync_dimension("authors", "dim_author", mapping)


def sync_dim_category(**context):
    mapping = {"_id_str": "category_id", "name": "name"}
    return sync_dimension("categories", "dim_category", mapping)


def sync_bridge(**context):
    mapping = {"_id_str": "book_id", "authorIds": "author_ids"}
    return sync_dimension("books", "bridge_book_author", mapping)


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
    return sync_fact("loans", "fact_loan", mapping, date_field="createdAt")


def sync_fact_payment(**context):
    mapping = {
        "_id_str": "payment_id",
        "memberId": "user_id",
        "loanId": "loan_id",
        "amount": "amount",
        "paidAt": "payment_time",
    }
    return sync_fact("fine_records", "fact_payment", mapping, date_field="paidAt")


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
    return sync_fact("book_reviews", "fact_ratings", mapping, date_field="createdAt")


def sync_fact_wishlist(**context):
    mapping = {
        "_id_str": "wishlist_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "addedAt": "added_at",
    }
    return sync_fact("wishlists", "fact_wishlist", mapping, date_field="addedAt")


def sync_fact_search(**context):
    mapping = {
        "_id_str": "search_id",
        "memberId": "user_id",
        "query": "query",
        "createdAt": "created_at",
    }
    return sync_fact("search_logs", "fact_search", mapping, date_field="createdAt")


def sync_fact_reading_session(**context):
    mapping = {
        "_id_str": "session_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "sessionDuration": "session_duration",
        "pagesRead": "pages_read",
        "startedAt": "started_at",
        "endedAt": "ended_at",
    }
    return sync_fact("reading_sessions", "fact_reading_session", mapping, date_field="startedAt")


def sync_fact_reservation(**context):
    mapping = {
        "_id_str": "reservation_id",
        "memberId": "user_id",
        "bookId": "book_id",
        "bookIsbn": "isbn",
        "requestedAt": "requested_at",
        "expiryDate": "expiry_date",
        "status": "status",
        "positionInQueue": "position_in_queue",
    }
    return sync_fact("reservations", "fact_reservation", mapping, date_field="requestedAt")


def sync_fact_activity_log(**context):
    mapping = {
        "_id_str": "activity_id",
        "userId": "user_id",
        "action": "action",
        "resource": "resource",
        "timestamp": "timestamp",
        "ipAddress": "ip_address",
    }
    return sync_fact("activity_logs", "fact_activity_log", mapping, date_field="timestamp")


def sync_dim_publisher(**context):
    mapping = {
        "_id_str": "publisher_id",
        "name": "name",
        "location": "location",
        "foundedYear": "founded_year",
        "website": "website",
    }
    return sync_dimension("publishers", "dim_publisher", mapping)


def sync_dim_member_preferences(**context):
    mapping = {
        "_id_str": "preference_id",
        "memberId": "user_id",
        "preferredAuthors": "preferred_authors",
        "preferredCategories": "preferred_categories",
        "readingPaceWPM": "reading_pace_wpm",
        "preferredFormats": "preferred_formats",
        "updatedAt": "updated_at",
    }
    return sync_dimension("member_preferences", "dim_member_preferences", mapping)


def sync_fact_book_embedding(**context):
    mapping = {
        "_id_str": "embedding_id",
        "bookId": "book_id",
        "embedding": "embedding",
        "modelVersion": "model_version",
        "createdAt": "created_at",
    }
    return sync_fact("book_embeddings", "fact_book_embedding", mapping, date_field="createdAt")


def sync_fact_member_analytics(**context):
    mapping = {
        "_id_str": "analytics_id",
        "memberId": "user_id",
        "booksRead": "books_read",
        "totalFines": "total_fines",
        "activeLoans": "active_loans",
        "avgRatingGiven": "avg_rating_given",
        "updatedAt": "updated_at",
    }
    return sync_fact("member_analytics", "fact_member_analytics", mapping, date_field="updatedAt")


def sync_fact_chatbot_feedback(**context):
    mapping = {
        "_id_str": "feedback_id",
        "sessionId": "session_id",
        "memberId": "user_id",
        "rating": "rating",
        "feedback": "feedback_text",
        "timestamp": "timestamp",
    }
    return sync_fact("chatbot_feedbacks", "fact_chatbot_feedback", mapping, date_field="timestamp")


def sync_dim_date(**context):
    """Populate dim_date table with date dimension data."""
    try:
        logger.info("=== START SYNC: dim_date ===")
        ch = get_ch_client()
        try:
            existing = ch.query("SELECT count() FROM dim_date").result_rows[0][0]
            if existing > 0:
                logger.info(f"dim_date already populated with {existing} rows, skipping")
                return existing

            import datetime as dt
            start_date = dt.date(2020, 1, 1)
            end_date = dt.date(2030, 12, 31)

            day_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            month_names = ["January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"]
            seasons = ["Winter", "Winter", "Spring", "Spring", "Spring", "Summer",
                      "Summer", "Summer", "Autumn", "Autumn", "Autumn", "Winter"]

            rows = []
            current = start_date
            delta = dt.timedelta(days=1)
            while current <= end_date:
                date_key = int(current.strftime("%Y%m%d"))
                day_of_week = current.weekday()
                day_name = day_names[day_of_week]
                is_weekend = 1 if day_of_week >= 5 else 0
                is_holiday = 0
                md = (current.month, current.day)
                if md in [(1, 1), (7, 4), (12, 25), (12, 31)]:
                    is_holiday = 1
                fiscal_q = ((current.month - 1) // 3) + 1

                rows.append({
                    "date_key": date_key,
                    "full_date": current,
                    "day_of_week": day_of_week + 1,
                    "day_name": day_name,
                    "day_of_month": current.day,
                    "day_of_year": current.timetuple().tm_yday,
                    "week_of_year": current.isocalendar()[1],
                    "month_number": current.month,
                    "month_name": month_names[current.month - 1],
                    "quarter": ((current.month - 1) // 3) + 1,
                    "year": current.year,
                    "fiscal_quarter": fiscal_q,
                    "is_weekend": is_weekend,
                    "is_holiday": is_holiday,
                    "season": seasons[current.month - 1],
                })
                current += delta

            df = pd.DataFrame(rows)
            ch.insert_df("dim_date", df)
            logger.info(f"dim_date: Inserted {len(rows)} rows")
            return len(rows)
        finally:
            ch.close()
    except Exception as e:
        logger.error(f"dim_date sync failed: {e}")
        raise AirflowException("dim_date sync failed")


def data_quality_check(**context):
    ch = get_ch_client()
    try:
        checks = [
            ("dim_user", ch.query("SELECT count() FROM dim_user").result_rows[0][0]),
            ("dim_book", ch.query("SELECT count() FROM dim_book").result_rows[0][0]),
            ("dim_author", ch.query("SELECT count() FROM dim_author").result_rows[0][0]),
            ("dim_category", ch.query("SELECT count() FROM dim_category").result_rows[0][0]),
            ("dim_date", ch.query("SELECT count() FROM dim_date").result_rows[0][0]),
            ("dim_publisher", ch.query("SELECT count() FROM dim_publisher").result_rows[0][0]),
            ("dim_member_preferences", ch.query("SELECT count() FROM dim_member_preferences").result_rows[0][0]),
            ("fact_loan", ch.query("SELECT count() FROM fact_loan").result_rows[0][0]),
            ("fact_payment", ch.query("SELECT count() FROM fact_payment").result_rows[0][0]),
            ("fact_events", ch.query("SELECT count() FROM fact_events").result_rows[0][0]),
            ("fact_ratings", ch.query("SELECT count() FROM fact_ratings").result_rows[0][0]),
            ("fact_wishlist", ch.query("SELECT count() FROM fact_wishlist").result_rows[0][0]),
            ("fact_search", ch.query("SELECT count() FROM fact_search").result_rows[0][0]),
            ("fact_reading_session", ch.query("SELECT count() FROM fact_reading_session").result_rows[0][0]),
            ("fact_reservation", ch.query("SELECT count() FROM fact_reservation").result_rows[0][0]),
            ("fact_activity_log", ch.query("SELECT count() FROM fact_activity_log").result_rows[0][0]),
            ("fact_book_embedding", ch.query("SELECT count() FROM fact_book_embedding").result_rows[0][0]),
            ("fact_member_analytics", ch.query("SELECT count() FROM fact_member_analytics").result_rows[0][0]),
            ("fact_chatbot_feedback", ch.query("SELECT count() FROM fact_chatbot_feedback").result_rows[0][0]),
            ("bridge_book_author", ch.query("SELECT count() FROM bridge_book_author").result_rows[0][0]),
        ]
        for table_name, count in checks:
            logger.info(f"DQ: {table_name} = {count:,} rows")

        core_tables = [c for name, c in checks if name in (
            "dim_user", "dim_book", "dim_author", "dim_category", "dim_date", "dim_publisher", "fact_loan"
        )]
        if not all(c > 0 for c in core_tables):
            raise AirflowException("Data quality check failed - core tables are empty")
        logger.info("DQ CHECK PASSED")
        return checks
    finally:
        ch.close()


# ============================================================================
# DAG DEFINITION — Airflow 3.x compatible
# ============================================================================

default_args = {
    "owner": "dataeng",
    "depends_on_past": False,
    "retries": 3,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(hours=2),
}

with DAG(
    "mongodb_clickhouse_library_etl",
    default_args=default_args,
    description="MongoDB->ClickHouse ETL Pipeline (v3)",
    schedule="@daily",
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["etl", "prod", "library", "mongodb", "clickhouse"],
    max_active_runs=1,
) as dag:

    dim_user = PythonOperator(task_id="dim_user", python_callable=sync_dim_user)
    dim_book = PythonOperator(task_id="dim_book", python_callable=sync_dim_book)
    dim_author = PythonOperator(task_id="dim_author", python_callable=sync_dim_author)
    dim_category = PythonOperator(task_id="dim_category", python_callable=sync_dim_category)
    dim_publisher = PythonOperator(task_id="dim_publisher", python_callable=sync_dim_publisher)
    dim_member_preferences = PythonOperator(task_id="dim_member_preferences", python_callable=sync_dim_member_preferences)
    dim_date = PythonOperator(task_id="dim_date", python_callable=sync_dim_date)
    bridge_book_author = PythonOperator(task_id="bridge_book_author", python_callable=sync_bridge)

    fact_loan = PythonOperator(task_id="fact_loan", python_callable=sync_fact_loan)
    fact_payment = PythonOperator(task_id="fact_payment", python_callable=sync_fact_payment)
    fact_events = PythonOperator(task_id="fact_events", python_callable=sync_fact_events)
    fact_ratings = PythonOperator(task_id="fact_ratings", python_callable=sync_fact_ratings)
    fact_wishlist = PythonOperator(task_id="fact_wishlist", python_callable=sync_fact_wishlist)
    fact_search = PythonOperator(task_id="fact_search", python_callable=sync_fact_search)
    fact_reading_session = PythonOperator(task_id="fact_reading_session", python_callable=sync_fact_reading_session)
    fact_reservation = PythonOperator(task_id="fact_reservation", python_callable=sync_fact_reservation)
    fact_activity_log = PythonOperator(task_id="fact_activity_log", python_callable=sync_fact_activity_log)
    fact_book_embedding = PythonOperator(task_id="fact_book_embedding", python_callable=sync_fact_book_embedding)
    fact_member_analytics = PythonOperator(task_id="fact_member_analytics", python_callable=sync_fact_member_analytics)
    fact_chatbot_feedback = PythonOperator(task_id="fact_chatbot_feedback", python_callable=sync_fact_chatbot_feedback)

    dq_check = PythonOperator(task_id="data_quality_check", python_callable=data_quality_check)

    (
        [dim_user, dim_book, dim_author, dim_category, dim_publisher, dim_member_preferences, dim_date]
        >> bridge_book_author
        >> [fact_loan, fact_payment, fact_events, fact_ratings, fact_wishlist, fact_search,
            fact_reading_session, fact_reservation, fact_activity_log,
            fact_book_embedding, fact_member_analytics, fact_chatbot_feedback]
        >> dq_check
    )
