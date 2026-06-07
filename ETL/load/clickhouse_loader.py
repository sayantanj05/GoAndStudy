"""
ClickHouse data loading module
"""
import os
from typing import List, Dict, Any
from datetime import datetime
import pandas as pd
from config.database import clickhouse_conn

class ClickHouseLoader:
    """Load transformed data into ClickHouse tables"""
    
    def __init__(self):
        self.batch_size = int(os.getenv('BATCH_SIZE', 1000))
    
    def create_database_if_not_exists(self):
        """Create ClickHouse database if it doesn't exist"""
        try:
            query = f"CREATE DATABASE IF NOT EXISTS {clickhouse_conn.database}"
            clickhouse_conn.execute_query(query)
            print(f"✅ Database {clickhouse_conn.database} ready")
        except Exception as e:
            print(f"❌ Error creating database: {e}")
            raise
    
    def load_data(self, table_name: str, data: List[Dict]):
        """Load data into ClickHouse table"""
        if not data:
            print(f"⚠️  No data to load into {table_name}")
            return
        
        try:
            # Convert to DataFrame for easier handling
            df = pd.DataFrame(data)
            
            # Handle NaN values and data types
            df = self._clean_dataframe(df)
            
            # Get column names
            column_names = df.columns.tolist()
            
            # Convert DataFrame to list of tuples
            data_tuples = [tuple(row) for row in df.values]
            
            # Clear existing data if needed (optional)
            # self._truncate_table(table_name)
            
            # Insert data in batches
            for i in range(0, len(data_tuples), self.batch_size):
                batch = data_tuples[i:i + self.batch_size]
                clickhouse_conn.insert_data(table_name, batch, column_names)
                print(f"✅ Loaded batch {i//self.batch_size + 1} ({len(batch)} records) into {table_name}")
            
            print(f"✅ Successfully loaded {len(data)} records into {table_name}")
            
        except Exception as e:
            print(f"❌ Error loading data into {table_name}: {e}")
            raise
    
    def _clean_dataframe(self, df: pd.DataFrame) -> pd.DataFrame:
        """Clean DataFrame for ClickHouse compatibility"""
        # Replace None/NaN with appropriate defaults
        df = df.fillna('')
        
        # Convert numeric columns properly
        numeric_columns = ['average_rating', 'total_ratings', 'total_issues', 'available_copies',
                         'total_loans', 'active_loans', 'age', 'session_duration', 
                         'pages_read', 'renewal_count', 'fine_amount', 'overdue_days',
                         'reading_pace_wpm']
        
        for col in numeric_columns:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')
        
        # Convert datetime columns to proper format
        datetime_columns = ['created_at', 'updated_at', 'date_of_birth', 'issued_at', 
                         'due_date', 'returned_at', 'added_at', 'started_at', 
                         'ended_at', 'requested_at', 'expiry_date', 'timestamp']
        
        for col in datetime_columns:
            if col in df.columns:
                df[col] = pd.to_datetime(df[col], errors='coerce')
                # Replace NaT with None to avoid serialization issues
                df[col] = df[col].astype('object').where(df[col].notna(), None)
        
        # Handle array columns
        array_columns = ['preferred_genres', 'author_ids', 'category_ids', 'preferred_authors', 
                         'preferred_categories', 'preferred_formats', 'embedding']
        
        for col in array_columns:
            if col in df.columns:
                df[col] = df[col].apply(lambda x: x if isinstance(x, list) else [])
        
        return df
    
    def _truncate_table(self, table_name: str):
        """Truncate table before loading (optional)"""
        try:
            query = f"TRUNCATE TABLE {table_name}"
            clickhouse_conn.execute_query(query)
            print(f"🗑️  Truncated table {table_name}")
        except Exception as e:
            print(f"⚠️  Could not truncate table {table_name}: {e}")
    
    def load_all_tables(self, transformed_data: Dict[str, List[Dict]]):
        """Load all transformed data into ClickHouse tables"""
        # Load dimension tables first (due to foreign key dependencies)
        dimension_tables = [
            'dim_user',
            'dim_book', 
            'dim_author',
            'dim_category',
            'dim_publisher',
            'dim_member_preferences'
        ]
        
        fact_tables = [
            'fact_loan',
            'fact_payment',
            'fact_ratings',
            'fact_wishlist',
            'fact_search',
            'fact_reading_session',
            'fact_reservation',
            'fact_activity_log',
            'fact_book_embedding',
            'fact_member_analytics',
            'fact_chatbot_feedback',
            'fact_recommendation'
        ]
        
        print("\n🚀 Starting data load to ClickHouse...")
        
        # Load dimension tables
        print("\n📊 Loading dimension tables...")
        for table_name in dimension_tables:
            if table_name in transformed_data:
                print(f"\n📥 Loading {table_name}...")
                self.load_data(table_name, transformed_data[table_name])
        
        # Load fact tables
        print("\n📈 Loading fact tables...")
        for table_name in fact_tables:
            if table_name in transformed_data:
                print(f"\n📥 Loading {table_name}...")
                self.load_data(table_name, transformed_data[table_name])
        
        print("\n✅ All data loaded successfully!")
    
    def verify_data_load(self):
        """Verify data was loaded correctly"""
        verification_queries = {
            'dim_user': 'SELECT COUNT(*) as count FROM dim_user',
            'dim_book': 'SELECT COUNT(*) as count FROM dim_book',
            'fact_loan': 'SELECT COUNT(*) as count FROM fact_loan',
            'fact_ratings': 'SELECT COUNT(*) as count FROM fact_ratings'
        }
        
        print("\n🔍 Verifying data load...")
        for table, query in verification_queries.items():
            try:
                result = clickhouse_conn.execute_query(query)
                count = result.result_rows[0][0] if result.result_rows else 0
                print(f"📊 {table}: {count:,} records")
            except Exception as e:
                print(f"❌ Error verifying {table}: {e}")
    
    def create_date_dimension(self):
        """Create and populate date dimension table"""
        print("\n📅 Creating date dimension...")
        
        try:
            # Generate date dimension data
            dates = []
            start_date = datetime(2020, 1, 1)
            end_date = datetime(2030, 12, 31)
            
            current_date = start_date
            while current_date <= end_date:
                date_key = int(current_date.strftime('%Y%m%d'))
                
                date_record = {
                    'date_key': int(date_key),
                    'full_date': current_date.date(),
                    'day_of_week': int(current_date.weekday() + 1),
                    'day_name': current_date.strftime('%A'),
                    'day_of_month': int(current_date.day),
                    'day_of_year': int(current_date.timetuple().tm_yday),
                    'week_of_year': int(current_date.isocalendar()[1]),
                    'month_number': int(current_date.month),
                    'month_name': current_date.strftime('%B'),
                    'quarter': int((current_date.month - 1) // 3 + 1),
                    'year': int(current_date.year),
                    'fiscal_quarter': int(((current_date.month - 1) % 12) // 3 + 1),
                    'is_weekend': int(1 if current_date.weekday() >= 5 else 0),
                    'is_holiday': int(0),  # Can be enhanced with actual holidays
                }
                dates.append(date_record)
                current_date += pd.Timedelta(days=1)
            
            # Load date dimension
            self.load_data('dim_date', dates)
            print(f"✅ Created date dimension with {len(dates)} dates")
            
        except Exception as e:
            print(f"❌ Error creating date dimension: {e}")
            raise
    
    def _get_season(self, month: int) -> str:
        """Get season from month"""
        if month in [12, 1, 2]:
            return 'Winter'
        elif month in [3, 4, 5]:
            return 'Spring'
        elif month in [6, 7, 8]:
            return 'Summer'
        else:
            return 'Fall'
