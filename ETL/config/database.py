"""
Database connection configuration for MongoDB and ClickHouse
"""
import os
from typing import Optional
from dotenv import load_dotenv
import pymongo
import clickhouse_connect

# Load environment variables
load_dotenv()

class MongoDBConnection:
    """MongoDB Atlas connection manager"""
    
    def __init__(self):
        self.uri = os.getenv('MONGO_URI')
        self.db_name = os.getenv('MONGO_DB_NAME', 'GoAndStudy')
        self.client: Optional[pymongo.MongoClient] = None
        self.database = None
    
    def connect(self):
        """Establish MongoDB connection"""
        try:
            self.client = pymongo.MongoClient(self.uri)
            # Test connection
            self.client.admin.command('ping')
            self.database = self.client[self.db_name]
            print(f"✅ Connected to MongoDB Atlas: {self.db_name}")
            return True
        except Exception as e:
            print(f"❌ MongoDB connection failed: {e}")
            return False
    
    def disconnect(self):
        """Close MongoDB connection"""
        if self.client:
            self.client.close()
            print("🔌 MongoDB connection closed")
    
    def get_collection(self, collection_name: str):
        """Get MongoDB collection"""
        if self.database is not None:
            return self.database[collection_name]
        raise ConnectionError("MongoDB not connected. Call connect() first.")

class ClickHouseConnection:
    """ClickHouse connection manager"""
    
    def __init__(self):
        self.host = os.getenv('CLICKHOUSE_HOST', 'localhost')
        self.port = int(os.getenv('CLICKHOUSE_PORT', 8123))
        self.user = os.getenv('CLICKHOUSE_USER', 'default')
        self.password = os.getenv('CLICKHOUSE_PASSWORD', '')
        self.database = os.getenv('CLICKHOUSE_DB', 'library_dw')
        self.client: Optional[clickhouse_connect.driver.Client] = None
    
    def connect(self):
        """Establish ClickHouse connection"""
        try:
            self.client = clickhouse_connect.get_client(
                host=self.host,
                port=self.port,
                username=self.user,
                password=self.password,
                database=self.database
            )
            # Test connection
            self.client.command('SELECT 1')
            print(f"✅ Connected to ClickHouse: {self.database}")
            return True
        except Exception as e:
            print(f"❌ ClickHouse connection failed: {e}")
            return False
    
    def disconnect(self):
        """Close ClickHouse connection"""
        if self.client:
            self.client.close()
            print("🔌 ClickHouse connection closed")
    
    def execute_query(self, query: str, parameters: dict = None):
        """Execute ClickHouse query"""
        if not self.client:
            raise ConnectionError("ClickHouse not connected. Call connect() first.")
        
        try:
            return self.client.query(query, parameters)
        except Exception as e:
            print(f"❌ Query execution failed: {e}")
            raise
    
    def insert_data(self, table: str, data: list, column_names: list = None):
        """Insert data into ClickHouse table"""
        if not self.client:
            raise ConnectionError("ClickHouse not connected. Call connect() first.")
        
        try:
            self.client.insert(table, data, column_names)
            print(f"✅ Inserted {len(data)} records into {table}")
        except Exception as e:
            print(f"❌ Data insertion failed: {e}")
            raise

# Global connection instances
mongo_conn = MongoDBConnection()
clickhouse_conn = ClickHouseConnection()
