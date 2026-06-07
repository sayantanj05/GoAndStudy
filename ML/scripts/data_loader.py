"""Data loader for ClickHouse interactions."""
import clickhouse_connect
import pandas as pd
import yaml
import os

CONFIG_PATH = os.path.join(os.path.dirname(__file__), '..', 'config', 'config.yaml')
with open(CONFIG_PATH) as f:
    config = yaml.safe_load(f)

CH = config["clickhouse"]

def get_client():
    return clickhouse_connect.get_client(
        host=CH["host"], port=CH["port"],
        username=CH["username"], password=CH["password"],
        database=CH["database"]
    )

def query_to_df(client, query):
    """Execute query and return DataFrame."""
    result = client.query(query)
    rows = result.result_rows
    cols = result.column_names
    return pd.DataFrame(rows, columns=cols)

def load_interactions():
    """Load user-book interactions from fact_loan and fact_ratings."""
    client = get_client()
    
    query_ratings = """
        SELECT user_id, book_id, rating 
        FROM fact_ratings
    """
    
    query_loans = """
        SELECT user_id, book_id, 3.5 as rating
        FROM fact_loan
        WHERE status != 'RETURNED'
    """
    
    df_ratings = query_to_df(client, query_ratings)
    df_loans = query_to_df(client, query_loans)
    
    df = pd.concat([df_ratings, df_loans], ignore_index=True)
    df = df.sort_values('rating', ascending=False).drop_duplicates(
        subset=['user_id', 'book_id'], keep='first'
    )
    
    return df[['user_id', 'book_id', 'rating']]

def load_book_metadata():
    """Load book info for enriching recommendations."""
    client = get_client()
    query = "SELECT book_id, title, author FROM dim_book"
    return query_to_df(client, query)
