"""
Simple MongoDB -> ClickHouse sync DAG
Loads .env before accessing variables; uses lazy connections.
"""

from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator
from datetime import datetime, timedelta
import pandas as pd
import os
from pymongo import MongoClient
import clickhouse_connect
from dotenv import load_dotenv

# Load .env at module level but do NOT open connections here
env_path = os.path.join(os.path.dirname(__file__), "..", ".env")
if os.path.exists(env_path):
    load_dotenv(env_path)
else:
    load_dotenv()  # try CWD

default_args = {
    "owner": "sayan",
    "depends_on_past": False,
    "email_on_failure": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}


def etl_process(**kwargs):
    """ETL: MongoDB -> ClickHouse with error handling"""
    try:
        MONGO_URI = os.getenv("MONGO_URI")
        if not MONGO_URI:
            raise ValueError("MONGO_URI missing from environment - check .env file")

        CH_HOST = os.getenv("CLICKHOUSE_HOST", "clickhouse")
        CH_PORT = int(os.getenv("CLICKHOUSE_PORT", "8123"))
        CH_USER = os.getenv("CLICKHOUSE_USER", "default")
        CH_PASSWORD = os.getenv("CLICKHOUSE_PASSWORD", "")
        CH_DB = os.getenv("CLICKHOUSE_DB", "library_dw")
        DB_NAME = os.getenv("MONGO_DB_NAME", "goandstudydb")

        # MongoDB connection
        mongo_client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=10000, tlsAllowInvalidCertificates=True)
        db = mongo_client[DB_NAME]

        # Extract members
        members = list(db["members"].find({}, {"_id": 1, "name": 1, "email": 1}).limit(100))
        if not members:
            print("No members found")
            return

        df = pd.DataFrame(members)
        df["user_id"] = df["_id"].apply(lambda x: str(x))
        df = df[["user_id", "name", "email"]]
        mongo_client.close()

        # ClickHouse connection & load
        ch_client = clickhouse_connect.get_client(
            host=CH_HOST,
            port=CH_PORT,
            username=CH_USER,
            password=CH_PASSWORD,
            database=CH_DB,
        )
        ch_client.insert_df(table="dim_user", df=df, column_names=list(df.columns))
        ch_client.close()

        print(f"Successfully synced {len(df)} members to ClickHouse dim_user!")

    except Exception as e:
        print(f"ETL Process Error: {str(e)}")
        raise


with DAG(
    "mongodb_to_clickhouse_sync",
    default_args=default_args,
    description="Syncs library members from MongoDB to ClickHouse",
    schedule=None,
    start_date=datetime(2024, 11, 1),
    catchup=False,
    tags=["etl", "mongodb", "clickhouse"],
) as dag:

    run_sync = PythonOperator(
        task_id="sync_members_to_dim_user",
        python_callable=etl_process,
    )
