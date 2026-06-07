"""
Real-time ETL Service for MongoDB to ClickHouse synchronization.
Replaces Apache Airflow with a Python-based continuous sync.
"""

import os
import logging
import time
import threading
from datetime import datetime, timedelta
from typing import Optional, Dict, Any, List
from contextlib import contextmanager

import pandas as pd
from bson import ObjectId
from pymongo import MongoClient, ASCENDING
from pymongo.collection import Collection
from pymongo.database import Database
import clickhouse_connect
from clickhouse_connect.driver.client import Client as ClickHouseClient

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configuration
MONGO_URI = os.getenv("MONGO_URI", "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
MONGO_DB_NAME = os.getenv("MONGO_DB_NAME", "goandstudydb")
CLICKHOUSE_HOST = os.getenv("CLICKHOUSE_HOST", "localhost")
CLICKHOUSE_PORT = int(os.getenv("CLICKHOUSE_PORT", "8123"))
CLICKHOUSE_USER = os.getenv("CLICKHOUSE_USER", "default")
CLICKHOUSE_PASSWORD = os.getenv("CLICKHOUSE_PASSWORD", "")
CLICKHOUSE_DB = os.getenv("CLICKHOUSE_DB", "library_dw")

# Sync intervals (in seconds)
SYNC_INTERVALS = {
    'loans': 30,        # High frequency - loans change often
    'members': 60,      # Medium frequency
    'books': 300,       # Low frequency - books don't change often
    'searches': 30,     # High frequency
    'reservations': 60, # Medium frequency
    'activities': 30,   # High frequency
    'reviews': 60,      # Medium frequency
}

class CustomJSONEncoder:
    """Custom encoder for MongoDB objects"""
    @staticmethod
    def encode(obj):
        if isinstance(obj, datetime):
            return obj.isoformat()
        if isinstance(obj, ObjectId):
            return str(obj)
        raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


class RealtimeETLService:
    """
    Real-time ETL service that continuously syncs MongoDB data to ClickHouse.
    Uses change streams and polling for real-time updates.
    """
    
    def __init__(self):
        self.mongo_client: Optional[MongoClient] = None
        self.mongo_db: Optional[Database] = None
        self.ch_client: Optional[ClickHouseClient] = None
        self.running = False
        self.sync_threads: List[threading.Thread] = []
        self.last_sync_times: Dict[str, datetime] = {}
        self.sync_stats = {
            'total_records_synced': 0,
            'syncs_completed': 0,
            'errors': 0,
            'start_time': None
        }
    
    def connect(self) -> bool:
        """Connect to MongoDB and ClickHouse"""
        try:
            # Connect to MongoDB
            self.mongo_client = MongoClient(
                MONGO_URI,
                serverSelectionTimeoutMS=10000,
                tlsAllowInvalidCertificates=True
            )
            self.mongo_db = self.mongo_client[MONGO_DB_NAME]
            
            # Test MongoDB connection
            self.mongo_client.admin.command('ping')
            logger.info(f"Connected to MongoDB: {MONGO_DB_NAME}")
            
            # Connect to ClickHouse
            self.ch_client = clickhouse_connect.get_client(
                host=CLICKHOUSE_HOST,
                port=CLICKHOUSE_PORT,
                username=CLICKHOUSE_USER,
                password=CLICKHOUSE_PASSWORD,
                database=CLICKHOUSE_DB,
                settings={"max_execution_time": 300},
            )
            
            # Test ClickHouse connection
            result = self.ch_client.query("SELECT 1").result_rows
            logger.info(f"Connected to ClickHouse: {CLICKHOUSE_DB}")
            
            return True
            
        except Exception as e:
            logger.error(f"Connection failed: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from databases"""
        if self.mongo_client:
            self.mongo_client.close()
            logger.info("Disconnected from MongoDB")
        if self.ch_client:
            self.ch_client.close()
            logger.info("Disconnected from ClickHouse")
    
    def get_mongo_collection(self, name: str) -> Collection:
        """Get MongoDB collection"""
        return self.mongo_db[name]
    
    def get_clickhouse_client(self) -> ClickHouseClient:
        """Get ClickHouse client"""
        return self.ch_client
    
    def get_last_sync_time(self, collection: str) -> datetime:
        """Get last sync time for a collection"""
        if collection not in self.last_sync_times:
            # Default to 24 hours ago for initial sync
            return datetime.now() - timedelta(hours=24)
        return self.last_sync_times[collection]
    
    def update_last_sync_time(self, collection: str):
        """Update last sync time for a collection"""
        self.last_sync_times[collection] = datetime.now()
    
    def sync_collection(self, mongo_collection: str, ch_table: str, 
                       transform_func=None, query_filter: Optional[Dict] = None) -> int:
        """
        Sync a MongoDB collection to ClickHouse table.
        
        Args:
            mongo_collection: MongoDB collection name
            ch_table: ClickHouse table name
            transform_func: Optional function to transform documents
            query_filter: Optional MongoDB query filter
            
        Returns:
            Number of records synced
        """
        try:
            collection = self.get_mongo_collection(mongo_collection)
            
            # Get documents modified since last sync
            last_sync = self.get_last_sync_time(mongo_collection)
            
            # Build query - look for documents with updatedAt or use _id timestamp
            if query_filter:
                cursor = collection.find(query_filter)
            else:
                # Try to find documents updated since last sync
                time_filter = {
                    "$or": [
                        {"updatedAt": {"$gte": last_sync}},
                        {"createdAt": {"$gte": last_sync}},
                        {"timestamp": {"$gte": last_sync}},
                        {"_id": {"$gte": ObjectId.from_datetime(last_sync)}}
                    ]
                }
                cursor = collection.find(time_filter).limit(1000)
            
            documents = list(cursor)
            
            if not documents:
                logger.debug(f"No new documents in {mongo_collection}")
                return 0
            
            # Transform to DataFrame
            df = pd.DataFrame(documents)
            
            # Apply custom transformation if provided
            if transform_func:
                df = transform_func(df)
            
            if df.empty:
                return 0
            
            # Convert ObjectId to string
            for col in df.columns:
                if df[col].apply(lambda x: isinstance(x, ObjectId)).any():
                    df[col] = df[col].astype(str)
                # Handle datetime columns
                if df[col].dtype == 'object':
                    try:
                        df[col] = pd.to_datetime(df[col], errors='ignore')
                    except:
                        pass
            
            # Insert into ClickHouse
            records_synced = len(df)
            
            # Use ClickHouse's ReplacingMergeTree for upserts
            self.ch_client.insert_df(ch_table, df)
            
            self.update_last_sync_time(mongo_collection)
            self.sync_stats['total_records_synced'] += records_synced
            
            logger.info(f"Synced {records_synced} records from {mongo_collection} to {ch_table}")
            return records_synced
            
        except Exception as e:
            logger.error(f"Sync error for {mongo_collection}: {e}")
            self.sync_stats['errors'] += 1
            return 0
    
    def sync_loans(self) -> int:
        """Sync loans collection to fact_loan table"""
        def transform_loans(df):
            # Rename columns to match ClickHouse schema
            column_mapping = {
                '_id': 'loan_id',
                'memberId': 'user_id',
                'bookId': 'book_id',
                'issuedAt': 'loan_date',
                'dueDate': 'due_date',
                'returnedAt': 'return_date',
                'status': 'status',
                'fineAmount': 'fine_amount',
                'renewalCount': 'renewal_count',
            }
            df = df.rename(columns=column_mapping)
            
            # Create time_id from loan_date
            if 'loan_date' in df.columns:
                df['time_id'] = pd.to_datetime(df['loan_date']).dt.strftime('%Y%m%d').astype(int)
            
            # Select only columns that exist in fact_loan
            fact_loan_cols = [
                'loan_id', 'user_id', 'book_id', 'time_id', 'loan_date', 
                'due_date', 'return_date', 'status', 'fine_amount', 
                'renewal_count', 'is_overdue'
            ]
            return df[[col for col in fact_loan_cols if col in df.columns]]
        
        return self.sync_collection('loans', 'fact_loan', transform_loans)
    
    def sync_members(self) -> int:
        """Sync members collection to dim_user table"""
        def transform_members(df):
            column_mapping = {
                '_id': 'user_id',
                'name': 'user_name',
                'email': 'email',
                'phone': 'phone',
                'membershipType': 'membership_type',
                'dateOfBirth': 'date_of_birth',
                'registrationDate': 'registration_date',
                'isActive': 'is_active',
                'gender': 'gender',
            }
            df = df.rename(columns=column_mapping)
            
            # Calculate age
            if 'date_of_birth' in df.columns:
                df['age'] = pd.to_datetime(df['date_of_birth']).apply(
                    lambda x: (datetime.now() - x).days // 365 if pd.notna(x) else 0
                )
            
            dim_user_cols = [
                'user_id', 'user_name', 'email', 'phone', 'membership_type',
                'date_of_birth', 'age', 'registration_date', 'is_active', 'gender'
            ]
            return df[[col for col in dim_user_cols if col in df.columns]]
        
        return self.sync_collection('members', 'dim_user', transform_members)
    
    def sync_books(self) -> int:
        """Sync books collection to dim_book table"""
        def transform_books(df):
            column_mapping = {
                '_id': 'book_id',
                'title': 'title',
                'author': 'author_name',
                'isbn': 'isbn',
                'category': 'category_name',
                'publisher': 'publisher',
                'publicationYear': 'publication_year',
                'totalCopies': 'total_copies',
                'availableCopies': 'available_copies',
            }
            df = df.rename(columns=column_mapping)
            
            dim_book_cols = [
                'book_id', 'title', 'author_name', 'isbn', 'category_name',
                'publisher', 'publication_year', 'total_copies', 'available_copies'
            ]
            return df[[col for col in dim_book_cols if col in df.columns]]
        
        return self.sync_collection('books', 'dim_book', transform_books)
    
    def sync_searches(self) -> int:
        """Sync search logs to fact_search table"""
        def transform_searches(df):
            column_mapping = {
                '_id': 'search_id',
                'memberId': 'user_id',
                'query': 'search_query',
                'timestamp': 'search_timestamp',
                'resultsCount': 'results_count',
            }
            df = df.rename(columns=column_mapping)
            
            # Create time_id
            if 'search_timestamp' in df.columns:
                df['time_id'] = pd.to_datetime(df['search_timestamp']).dt.strftime('%Y%m%d').astype(int)
            
            fact_search_cols = [
                'search_id', 'user_id', 'time_id', 'search_query', 
                'search_timestamp', 'results_count'
            ]
            return df[[col for col in fact_search_cols if col in df.columns]]
        
        return self.sync_collection('searchLogs', 'fact_search', transform_searches)
    
    def sync_reservations(self) -> int:
        """Sync reservations to fact_reservation table"""
        def transform_reservations(df):
            column_mapping = {
                '_id': 'reservation_id',
                'memberId': 'user_id',
                'bookId': 'book_id',
                'reservedAt': 'reservation_date',
                'status': 'status',
                'fulfilledAt': 'fulfilled_date',
            }
            df = df.rename(columns=column_mapping)
            
            if 'reservation_date' in df.columns:
                df['time_id'] = pd.to_datetime(df['reservation_date']).dt.strftime('%Y%m%d').astype(int)
            
            fact_reservation_cols = [
                'reservation_id', 'user_id', 'book_id', 'time_id',
                'reservation_date', 'status', 'fulfilled_date'
            ]
            return df[[col for col in fact_reservation_cols if col in df.columns]]
        
        return self.sync_collection('reservations', 'fact_reservation', transform_reservations)
    
    def run_continuous_sync(self, collection_name: str, sync_func, interval: int):
        """Run continuous sync for a collection"""
        while self.running:
            try:
                sync_func()
                self.sync_stats['syncs_completed'] += 1
            except Exception as e:
                logger.error(f"Continuous sync error for {collection_name}: {e}")
                self.sync_stats['errors'] += 1
            
            # Wait before next sync
            time.sleep(interval)
    
    def start(self):
        """Start the real-time ETL service"""
        logger.info("Starting Real-time ETL Service...")
        
        if not self.connect():
            logger.error("Failed to connect to databases. ETL service not started.")
            return False
        
        self.running = True
        self.sync_stats['start_time'] = datetime.now()
        
        # Define sync tasks
        sync_tasks = [
            ('loans', self.sync_loans, SYNC_INTERVALS['loans']),
            ('members', self.sync_members, SYNC_INTERVALS['members']),
            ('books', self.sync_books, SYNC_INTERVALS['books']),
            ('searches', self.sync_searches, SYNC_INTERVALS['searches']),
            ('reservations', self.sync_reservations, SYNC_INTERVALS['reservations']),
        ]
        
        # Start sync threads
        for name, func, interval in sync_tasks:
            thread = threading.Thread(
                target=self.run_continuous_sync,
                args=(name, func, interval),
                daemon=True,
                name=f"Sync-{name}"
            )
            thread.start()
            self.sync_threads.append(thread)
            logger.info(f"Started sync thread for {name} (interval: {interval}s)")
        
        logger.info("Real-time ETL Service started successfully!")
        return True
    
    def stop(self):
        """Stop the ETL service"""
        logger.info("Stopping Real-time ETL Service...")
        self.running = False
        
        # Wait for threads to finish
        for thread in self.sync_threads:
            thread.join(timeout=5)
        
        self.disconnect()
        logger.info("Real-time ETL Service stopped.")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get sync statistics"""
        uptime = None
        if self.sync_stats['start_time']:
            uptime = (datetime.now() - self.sync_stats['start_time']).total_seconds()
        
        return {
            'running': self.running,
            'uptime_seconds': uptime,
            'total_records_synced': self.sync_stats['total_records_synced'],
            'syncs_completed': self.sync_stats['syncs_completed'],
            'errors': self.sync_stats['errors'],
            'last_sync_times': {k: v.isoformat() for k, v in self.last_sync_times.items()},
        }
    
    def force_sync_all(self) -> Dict[str, int]:
        """Force immediate sync of all collections"""
        logger.info("Force syncing all collections...")
        results = {}
        
        results['loans'] = self.sync_loans()
        results['members'] = self.sync_members()
        results['books'] = self.sync_books()
        results['searches'] = self.sync_searches()
        results['reservations'] = self.sync_reservations()
        
        logger.info(f"Force sync complete: {results}")
        return results


# Singleton instance
_etl_service: Optional[RealtimeETLService] = None

def get_etl_service() -> RealtimeETLService:
    """Get or create ETL service singleton"""
    global _etl_service
    if _etl_service is None:
        _etl_service = RealtimeETLService()
    return _etl_service


if __name__ == "__main__":
    # Run as standalone script
    service = RealtimeETLService()
    try:
        service.start()
        # Keep running
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        service.stop()
    except Exception as e:
        logger.error(f"ETL service error: {e}")
        service.stop()


"""
Real-time ETL Service for MongoDB to ClickHouse synchronization.
Replaces Apache Airflow with a Python-based continuous sync.
"""

import os
import logging
import time
import threading
from datetime import datetime, timedelta
from typing import Optional, Dict, Any, List
from contextlib import contextmanager

import pandas as pd
from bson import ObjectId
from pymongo import MongoClient, ASCENDING
from pymongo.collection import Collection
from pymongo.database import Database
import clickhouse_connect
from clickhouse_connect.driver.client import Client as ClickHouseClient

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configuration
MONGO_URI = os.getenv("MONGO_URI", "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
MONGO_DB_NAME = os.getenv("MONGO_DB_NAME", "goandstudydb")
CLICKHOUSE_HOST = os.getenv("CLICKHOUSE_HOST", "localhost")
CLICKHOUSE_PORT = int(os.getenv("CLICKHOUSE_PORT", "8123"))
CLICKHOUSE_USER = os.getenv("CLICKHOUSE_USER", "default")
CLICKHOUSE_PASSWORD = os.getenv("CLICKHOUSE_PASSWORD", "")
CLICKHOUSE_DB = os.getenv("CLICKHOUSE_DB", "library_dw")

# Sync intervals (in seconds)
SYNC_INTERVALS = {
    'loans': 30,        # High frequency - loans change often
    'members': 60,      # Medium frequency
    'books': 300,       # Low frequency - books don't change often
    'searches': 30,     # High frequency
    'reservations': 60, # Medium frequency
    'activities': 30,   # High frequency
    'reviews': 60,      # Medium frequency
}

class CustomJSONEncoder:
    """Custom encoder for MongoDB objects"""
    @staticmethod
    def encode(obj):
        if isinstance(obj, datetime):
            return obj.isoformat()
        if isinstance(obj, ObjectId):
            return str(obj)
        raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


class RealtimeETLService:
    """
    Real-time ETL service that continuously syncs MongoDB data to ClickHouse.
    Uses change streams and polling for real-time updates.
    """
    
    def __init__(self):
        self.mongo_client: Optional[MongoClient] = None
        self.mongo_db: Optional[Database] = None
        self.ch_client: Optional[ClickHouseClient] = None
        self.running = False
        self.sync_threads: List[threading.Thread] = []
        self.last_sync_times: Dict[str, datetime] = {}
        self.sync_stats = {
            'total_records_synced': 0,
            'syncs_completed': 0,
            'errors': 0,
            'start_time': None
        }
    
    def connect(self) -> bool:
        """Connect to MongoDB and ClickHouse"""
        try:
            # Connect to MongoDB
            self.mongo_client = MongoClient(
                MONGO_URI,
                serverSelectionTimeoutMS=10000,
                tlsAllowInvalidCertificates=True
            )
            self.mongo_db = self.mongo_client[MONGO_DB_NAME]
            
            # Test MongoDB connection
            self.mongo_client.admin.command('ping')
            logger.info(f"Connected to MongoDB: {MONGO_DB_NAME}")
            
            # Connect to ClickHouse
            self.ch_client = clickhouse_connect.get_client(
                host=CLICKHOUSE_HOST,
                port=CLICKHOUSE_PORT,
                username=CLICKHOUSE_USER,
                password=CLICKHOUSE_PASSWORD,
                database=CLICKHOUSE_DB,
                settings={"max_execution_time": 300},
            )
            
            # Test ClickHouse connection
            result = self.ch_client.query("SELECT 1").result_rows
            logger.info(f"Connected to ClickHouse: {CLICKHOUSE_DB}")
            
            return True
            
        except Exception as e:
            logger.error(f"Connection failed: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from databases"""
        if self.mongo_client:
            self.mongo_client.close()
            logger.info("Disconnected from MongoDB")
        if self.ch_client:
            self.ch_client.close()
            logger.info("Disconnected from ClickHouse")
    
    def get_mongo_collection(self, name: str) -> Collection:
        """Get MongoDB collection"""
        return self.mongo_db[name]
    
    def get_clickhouse_client(self) -> ClickHouseClient:
        """Get ClickHouse client"""
        return self.ch_client
    
    def get_last_sync_time(self, collection: str) -> datetime:
        """Get last sync time for a collection"""
        if collection not in self.last_sync_times:
            # Default to 24 hours ago for initial sync
            return datetime.now() - timedelta(hours=24)
        return self.last_sync_times[collection]
    
    def update_last_sync_time(self, collection: str):
        """Update last sync time for a collection"""
        self.last_sync_times[collection] = datetime.now()
    
    def sync_collection(self, mongo_collection: str, ch_table: str, 
                       transform_func=None, query_filter: Optional[Dict] = None) -> int:
        """
        Sync a MongoDB collection to ClickHouse table.
        
        Args:
            mongo_collection: MongoDB collection name
            ch_table: ClickHouse table name
            transform_func: Optional function to transform documents
            query_filter: Optional MongoDB query filter
            
        Returns:
            Number of records synced
        """
        try:
            collection = self.get_mongo_collection(mongo_collection)
            
            # Get documents modified since last sync
            last_sync = self.get_last_sync_time(mongo_collection)
            
            # Build query - look for documents with updatedAt or use _id timestamp
            if query_filter:
                cursor = collection.find(query_filter)
            else:
                # Try to find documents updated since last sync
                time_filter = {
                    "$or": [
                        {"updatedAt": {"$gte": last_sync}},
                        {"createdAt": {"$gte": last_sync}},
                        {"timestamp": {"$gte": last_sync}},
                        {"_id": {"$gte": ObjectId.from_datetime(last_sync)}}
                    ]
                }
                cursor = collection.find(time_filter).limit(1000)
            
            documents = list(cursor)
            
            if not documents:
                logger.debug(f"No new documents in {mongo_collection}")
                return 0
            
            # Transform to DataFrame
            df = pd.DataFrame(documents)
            
            # Apply custom transformation if provided
            if transform_func:
                df = transform_func(df)
            
            if df.empty:
                return 0
            
            # Convert ObjectId to string
            for col in df.columns:
                if df[col].apply(lambda x: isinstance(x, ObjectId)).any():
                    df[col] = df[col].astype(str)
                # Handle datetime columns
                if df[col].dtype == 'object':
                    try:
                        df[col] = pd.to_datetime(df[col], errors='ignore')
                    except:
                        pass
            
            # Insert into ClickHouse
            records_synced = len(df)
            
            # Use ClickHouse's ReplacingMergeTree for upserts
            self.ch_client.insert_df(ch_table, df)
            
            self.update_last_sync_time(mongo_collection)
            self.sync_stats['total_records_synced'] += records_synced
            
            logger.info(f"Synced {records_synced} records from {mongo_collection} to {ch_table}")
            return records_synced
            
        except Exception as e:
            logger.error(f"Sync error for {mongo_collection}: {e}")
            self.sync_stats['errors'] += 1
            return 0
    
    def sync_loans(self) -> int:
        """Sync loans collection to fact_loan table"""
        def transform_loans(df):
            # Rename columns to match ClickHouse schema
            column_mapping = {
                '_id': 'loan_id',
                'memberId': 'user_id',
                'bookId': 'book_id',
                'issuedAt': 'loan_date',
                'dueDate': 'due_date',
                'returnedAt': 'return_date',
                'status': 'status',
                'fineAmount': 'fine_amount',
                'renewalCount': 'renewal_count',
            }
            df = df.rename(columns=column_mapping)
            
            # Create time_id from loan_date
            if 'loan_date' in df.columns:
                df['time_id'] = pd.to_datetime(df['loan_date']).dt.strftime('%Y%m%d').astype(int)
            
            # Select only columns that exist in fact_loan
            fact_loan_cols = [
                'loan_id', 'user_id', 'book_id', 'time_id', 'loan_date', 
                'due_date', 'return_date', 'status', 'fine_amount', 
                'renewal_count', 'is_overdue'
            ]
            return df[[col for col in fact_loan_cols if col in df.columns]]
        
        return self.sync_collection('loans', 'fact_loan', transform_loans)
    
    def sync_members(self) -> int:
        """Sync members collection to dim_user table"""
        def transform_members(df):
            column_mapping = {
                '_id': 'user_id',
                'name': 'user_name',
                'email': 'email',
                'phone': 'phone',
                'membershipType': 'membership_type',
                'dateOfBirth': 'date_of_birth',
                'registrationDate': 'registration_date',
                'isActive': 'is_active',
                'gender': 'gender',
            }
            df = df.rename(columns=column_mapping)
            
            # Calculate age
            if 'date_of_birth' in df.columns:
                df['age'] = pd.to_datetime(df['date_of_birth']).apply(
                    lambda x: (datetime.now() - x).days // 365 if pd.notna(x) else 0
                )
            
            dim_user_cols = [
                'user_id', 'user_name', 'email', 'phone', 'membership_type',
                'date_of_birth', 'age', 'registration_date', 'is_active', 'gender'
            ]
            return df[[col for col in dim_user_cols if col in df.columns]]
        
        return self.sync_collection('members', 'dim_user', transform_members)
    
    def sync_books(self) -> int:
        """Sync books collection to dim_book table"""
        def transform_books(df):
            column_mapping = {
                '_id': 'book_id',
                'title': 'title',
                'author': 'author_name',
                'isbn': 'isbn',
                'category': 'category_name',
                'publisher': 'publisher',
                'publicationYear': 'publication_year',
                'totalCopies': 'total_copies',
                'availableCopies': 'available_copies',
            }
            df = df.rename(columns=column_mapping)
            
            dim_book_cols = [
                'book_id', 'title', 'author_name', 'isbn', 'category_name',
                'publisher', 'publication_year', 'total_copies', 'available_copies'
            ]
            return df[[col for col in dim_book_cols if col in df.columns]]
        
        return self.sync_collection('books', 'dim_book', transform_books)
    
    def sync_searches(self) -> int:
        """Sync search logs to fact_search table"""
        def transform_searches(df):
            column_mapping = {
                '_id': 'search_id',
                'memberId': 'user_id',
                'query': 'search_query',
                'timestamp': 'search_timestamp',
                'resultsCount': 'results_count',
            }
            df = df.rename(columns=column_mapping)
            
            # Create time_id
            if 'search_timestamp' in df.columns:
                df['time_id'] = pd.to_datetime(df['search_timestamp']).dt.strftime('%Y%m%d').astype(int)
            
            fact_search_cols = [
                'search_id', 'user_id', 'time_id', 'search_query', 
                'search_timestamp', 'results_count'
            ]
            return df[[col for col in fact_search_cols if col in df.columns]]
        
        return self.sync_collection('searchLogs', 'fact_search', transform_searches)
    
    def sync_reservations(self) -> int:
        """Sync reservations to fact_reservation table"""
        def transform_reservations(df):
            column_mapping = {
                '_id': 'reservation_id',
                'memberId': 'user_id',
                'bookId': 'book_id',
                'reservedAt': 'reservation_date',
                'status': 'status',
                'fulfilledAt': 'fulfilled_date',
            }
            df = df.rename(columns=column_mapping)
            
            if 'reservation_date' in df.columns:
                df['time_id'] = pd.to_datetime(df['reservation_date']).dt.strftime('%Y%m%d').astype(int)
            
            fact_reservation_cols = [
                'reservation_id', 'user_id', 'book_id', 'time_id',
                'reservation_date', 'status', 'fulfilled_date'
            ]
            return df[[col for col in fact_reservation_cols if col in df.columns]]
        
        return self.sync_collection('reservations', 'fact_reservation', transform_reservations)
    
    def run_continuous_sync(self, collection_name: str, sync_func, interval: int):
        """Run continuous sync for a collection"""
        while self.running:
            try:
                sync_func()
                self.sync_stats['syncs_completed'] += 1
            except Exception as e:
                logger.error(f"Continuous sync error for {collection_name}: {e}")
                self.sync_stats['errors'] += 1
            
            # Wait before next sync
            time.sleep(interval)
    
    def start(self):
        """Start the real-time ETL service"""
        logger.info("Starting Real-time ETL Service...")
        
        if not self.connect():
            logger.error("Failed to connect to databases. ETL service not started.")
            return False
        
        self.running = True
        self.sync_stats['start_time'] = datetime.now()
        
        # Define sync tasks
        sync_tasks = [
            ('loans', self.sync_loans, SYNC_INTERVALS['loans']),
            ('members', self.sync_members, SYNC_INTERVALS['members']),
            ('books', self.sync_books, SYNC_INTERVALS['books']),
            ('searches', self.sync_searches, SYNC_INTERVALS['searches']),
            ('reservations', self.sync_reservations, SYNC_INTERVALS['reservations']),
        ]
        
        # Start sync threads
        for name, func, interval in sync_tasks:
            thread = threading.Thread(
                target=self.run_continuous_sync,
                args=(name, func, interval),
                daemon=True,
                name=f"Sync-{name}"
            )
            thread.start()
            self.sync_threads.append(thread)
            logger.info(f"Started sync thread for {name} (interval: {interval}s)")
        
        logger.info("Real-time ETL Service started successfully!")
        return True
    
    def stop(self):
        """Stop the ETL service"""
        logger.info("Stopping Real-time ETL Service...")
        self.running = False
        
        # Wait for threads to finish
        for thread in self.sync_threads:
            thread.join(timeout=5)
        
        self.disconnect()
        logger.info("Real-time ETL Service stopped.")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get sync statistics"""
        uptime = None
        if self.sync_stats['start_time']:
            uptime = (datetime.now() - self.sync_stats['start_time']).total_seconds()
        
        return {
            'running': self.running,
            'uptime_seconds': uptime,
            'total_records_synced': self.sync_stats['total_records_synced'],
            'syncs_completed': self.sync_stats['syncs_completed'],
            'errors': self.sync_stats['errors'],
            'last_sync_times': {k: v.isoformat() for k, v in self.last_sync_times.items()},
        }
    
    def force_sync_all(self) -> Dict[str, int]:
        """Force immediate sync of all collections"""
        logger.info("Force syncing all collections...")
        results = {}
        
        results['loans'] = self.sync_loans()
        results['members'] = self.sync_members()
        results['books'] = self.sync_books()
        results['searches'] = self.sync_searches()
        results['reservations'] = self.sync_reservations()
        
        logger.info(f"Force sync complete: {results}")
        return results


# Singleton instance
_etl_service: Optional[RealtimeETLService] = None

def get_etl_service() -> RealtimeETLService:
    """Get or create ETL service singleton"""
    global _etl_service
    if _etl_service is None:
        _etl_service = RealtimeETLService()
    return _etl_service


if __name__ == "__main__":
    # Run as standalone script
    service = RealtimeETLService()
    try:
        service.start()
        # Keep running
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        service.stop()
    except Exception as e:
        logger.error(f"ETL service error: {e}")
        service.stop()
