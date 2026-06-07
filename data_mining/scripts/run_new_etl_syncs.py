"""
Standalone ETL runner for new Phase 1 tables.
Populates: dim_publisher, dim_member_preferences, fact_reading_session,
           fact_reservation, fact_activity_log, fact_book_embedding,
           fact_member_analytics, fact_chatbot_feedback
"""

import os
import logging
import json
import datetime as dt
import pandas as pd
from bson import ObjectId

from pymongo import MongoClient
import clickhouse_connect
from contextlib import contextmanager

class CustomJSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, dt.datetime):
            return obj.isoformat()
        if isinstance(obj, ObjectId):
            return str(obj)
        return super().default(obj)

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


os.environ.setdefault("MONGO_URI", "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
os.environ.setdefault("MONGO_DB_NAME", "goandstudydb")
os.environ.setdefault("CLICKHOUSE_HOST", "localhost")
os.environ.setdefault("CLICKHOUSE_PORT", "8123")
os.environ.setdefault("CLICKHOUSE_USER", "default")
os.environ.setdefault("CLICKHOUSE_PASSWORD", "")
os.environ.setdefault("CLICKHOUSE_DB", "library_dw")

@contextmanager
def get_mongo_connection():
    mongo_uri = os.getenv("MONGO_URI")
    client = MongoClient(mongo_uri, serverSelectionTimeoutMS=10000, tlsAllowInvalidCertificates=True)
    db = client[os.getenv("MONGO_DB_NAME", "goandstudydb")]
    try:
        yield client, db
    finally:
        client.close()

def get_ch_client():
    return clickhouse_connect.get_client(
        host=os.getenv("CLICKHOUSE_HOST", "localhost"),
        port=int(os.getenv("CLICKHOUSE_PORT", "8123")),
        username=os.getenv("CLICKHOUSE_USER", "default"),
        password=os.getenv("CLICKHOUSE_PASSWORD", ""),
        database=os.getenv("CLICKHOUSE_DB", "library_dw"),
        settings={"max_execution_time": 300},
    )

def get_schema_types(table: str) -> dict:
    ch = get_ch_client()
    try:
        rows = ch.query(f"DESCRIBE {table}").result_rows
        result = {}
        for r in rows:
            col_name = r[0]
            col_type = r[1]
            is_nullable = col_type.startswith("Nullable(")
            result[col_name] = (col_type, is_nullable)
        return result
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
    """
    Create time_id from date_field. If date_field is missing or all null,
    fallback to MongoDB ObjectId generation timestamp.
    """
    has_valid_dates = False
    if date_field in df.columns:
        non_null = df[date_field].dropna()
        has_valid_dates = len(non_null) > 0
    
    if has_valid_dates:
        df["time_id"] = (
            pd.to_datetime(df[date_field], errors="coerce")
            .dt.strftime("%Y%m%d")
            .astype("Int64")
            .fillna(0)
        )
    else:
        # Fallback: extract timestamp from MongoDB ObjectId
        def extract_oid_ts(oid):
            try:
                if isinstance(oid, ObjectId):
                    return int(oid.generation_time.strftime("%Y%m%d"))
                return int(ObjectId(str(oid)).generation_time.strftime("%Y%m%d"))
            except Exception:
                return int(dt.datetime.now().strftime("%Y%m%d"))
        
        id_col = "_id" if "_id" in df.columns else "id"
        df["time_id"] = df[id_col].apply(extract_oid_ts)
    
    return df

def fill_missing_columns(df: pd.DataFrame, schema_types: dict) -> pd.DataFrame:
    for col, (ch_type, is_nullable) in schema_types.items():
        if col in df.columns:
            continue
        if is_nullable:
            df[col] = None
        elif "Int" in ch_type or "Float" in ch_type or "Decimal" in ch_type or "Double" in ch_type:
            df[col] = 0
        elif "String" in ch_type:
            df[col] = ""
        elif "Array" in ch_type:
            df[col] = df.apply(lambda _: [], axis=1)
        elif "Date" in ch_type:
            df[col] = pd.Timestamp("1970-01-01")
        elif "Enum" in ch_type:
            df[col] = ""
        else:
            df[col] = None
    return df

def sync_dimension(collection: str, table: str, mapping: dict):
    logger.info(f"=== SYNC DIMENSION: {collection} -> {table} ===")
    schema_types = get_schema_types(table)
    schema_cols = list(schema_types.keys())
    
    with get_mongo_connection() as (_, db):
        coll = db[collection]
        total = coll.count_documents({})
        logger.info(f"{table}: MongoDB {collection} total={total}")
        
        if total == 0:
            logger.info(f"{table}: Collection empty, skipping")
            return 0
        
        cursor = coll.find({}).batch_size(2000)
        batch = []
        row_count = 0
        
        for doc in cursor:
            batch.append(doc)
            if len(batch) >= 2000:
                row_count += _insert_batch(table, batch, mapping, schema_types, schema_cols)
                batch = []
        
        if batch:
            row_count += _insert_batch(table, batch, mapping, schema_types, schema_cols)
    
    logger.info(f"{table}: COMPLETED - inserted={row_count}")
    return row_count

def sync_fact(collection: str, table: str, mapping: dict, date_field: str):
    logger.info(f"=== SYNC FACT: {collection} -> {table} ===")
    schema_types = get_schema_types(table)
    schema_cols = list(schema_types.keys())
    
    with get_mongo_connection() as (_, db):
        coll = db[collection]
        total = coll.count_documents({})
        logger.info(f"{table}: MongoDB {collection} total={total}")
        
        if total == 0:
            logger.info(f"{table}: Collection empty, skipping")
            return 0
        
        cursor = coll.find({}).batch_size(2000)
        batch = []
        row_count = 0
        
        for doc in cursor:
            batch.append(doc)
            if len(batch) >= 2000:
                row_count += _insert_fact_batch(table, batch, mapping, schema_types, schema_cols, date_field)
                batch = []
        
        if batch:
            row_count += _insert_fact_batch(table, batch, mapping, schema_types, schema_cols, date_field)
    
    logger.info(f"{table}: COMPLETED - inserted={row_count}")
    return row_count

def _insert_batch(table, batch, mapping, schema_types, schema_cols):
    df = pd.DataFrame(batch)
    df = safe_nulls(df)
    id_col = "_id" if "_id" in df.columns else "id"
    df["_id_str"] = df[id_col].astype(str)
    df = df.rename(columns=mapping)
    df = fill_missing_columns(df, schema_types)
    df_final = df[schema_cols]
    ch = get_ch_client()
    try:
        ch.insert_df(table, df_final)
    finally:
        ch.close()
    return len(df_final)

def _insert_fact_batch(table, batch, mapping, schema_types, schema_cols, date_field):
    df = pd.DataFrame(batch)
    df = safe_nulls(df)
    # Serialize complex objects (lists/dicts) to JSON strings for ClickHouse String columns
    for col in df.columns:
        if col in schema_types:
            ch_type, _ = schema_types[col]
            if "Array" in ch_type:
                continue  # Keep native lists for Array columns
        if df[col].apply(lambda x: isinstance(x, (list, dict))).any():
            df[col] = df[col].apply(lambda x: json.dumps(x, cls=CustomJSONEncoder) if isinstance(x, (list, dict)) else x)


    df = make_time_id(df, date_field)

    id_col = "_id" if "_id" in df.columns else "id"
    df["_id_str"] = df[id_col].astype(str)
    df = df.rename(columns=mapping)
    # Special case: member_analytics _id IS the user_id (FK to dim_user)
    if table == "fact_member_analytics" and "analytics_id" in df.columns:
        df["user_id"] = df["analytics_id"]
    df = fill_missing_columns(df, schema_types)
    df_final = df[schema_cols]
    ch = get_ch_client()
    try:
        ch.insert_df(table, df_final)
    finally:
        ch.close()
    return len(df_final)

def truncate_tables(tables: list):
    ch = get_ch_client()
    try:
        for table in tables:
            ch.command(f"TRUNCATE TABLE {table}")
            logger.info(f"Truncated table: {table}")
    finally:
        ch.close()

def main():
    # Truncate affected tables to re-sync with fixed mappings
    truncate_tables([
        "fact_reading_session",
        "fact_activity_log",
        "fact_member_analytics"
    ])
    
    results = {}
    
    results["dim_publisher"] = sync_dimension("publishers", "dim_publisher", {
        "_id_str": "publisher_id", "name": "name", "location": "location",
        "foundedYear": "founded_year", "website": "website"
    })
    
    results["dim_member_preferences"] = sync_dimension("member_preferences", "dim_member_preferences", {
        "_id_str": "preference_id", "memberId": "user_id", "preferredAuthors": "preferred_authors",
        "preferredCategories": "preferred_categories", "readingPaceWPM": "reading_pace_wpm",
        "preferredFormats": "preferred_formats", "updatedAt": "updated_at"
    })
    
    results["fact_reading_session"] = sync_fact("reading_sessions", "fact_reading_session", {
        "_id_str": "session_id", "memberId": "user_id", "bookId": "book_id",
        "sessionDuration": "session_duration", "pagesRead": "pages_read",
        "startedAt": "started_at", "endedAt": "ended_at"
    }, date_field="startedAt")
    
    results["fact_reservation"] = sync_fact("reservations", "fact_reservation", {
        "_id_str": "reservation_id", "memberId": "user_id", "bookId": "book_id",
        "bookIsbn": "isbn", "requestedAt": "requested_at", "expiryDate": "expiry_date",
        "status": "status", "positionInQueue": "position_in_queue"
    }, date_field="requestedAt")
    
    results["fact_activity_log"] = sync_fact("activity_logs", "fact_activity_log", {
        "_id_str": "activity_id", "actorId": "user_id", "eventType": "action",
        "targetCollection": "resource", "createdAt": "timestamp"
    }, date_field="createdAt")
    
    results["fact_book_embedding"] = sync_fact("book_embeddings", "fact_book_embedding", {
        "_id_str": "embedding_id", "bookId": "book_id", "embedding": "embedding",
        "modelVersion": "model_version", "createdAt": "created_at"
    }, date_field="createdAt")
    
    results["fact_member_analytics"] = sync_fact("member_analytics", "fact_member_analytics", {
        "_id_str": "analytics_id", "totalLoans": "total_loans",
        "totalReturned": "total_returned", "totalOverdue": "total_overdue",
        "overdueRate": "overdue_rate", "totalFinesIncurred": "total_fines_incurred",
        "totalFinesPaid": "total_fines_paid", "avgLoanDuration": "avg_loan_duration",
        "totalSearches": "total_searches", "totalAiCalls": "total_ai_calls",
        "aiConversionRate": "ai_conversion_rate", "mostActiveMonth": "most_active_month",
        "genreBreakdown": "genre_breakdown", "computedAt": "computed_at", "age": "age"
    }, date_field="computedAt")
    
    results["fact_chatbot_feedback"] = sync_fact("chatbot_feedbacks", "fact_chatbot_feedback", {
        "_id_str": "feedback_id", "sessionId": "session_id", "memberId": "user_id",
        "rating": "rating", "feedback": "feedback_text", "timestamp": "timestamp"
    }, date_field="timestamp")
    
    logger.info("=" * 60)
    logger.info("ETL SYNC SUMMARY")
    logger.info("=" * 60)
    for table, count in results.items():
        logger.info(f"  {table}: {count} rows")
    logger.info("=" * 60)

if __name__ == "__main__":
    main()
