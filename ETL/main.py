"""
Main ETL Pipeline Orchestrator
MongoDB Atlas to ClickHouse Data Warehouse
"""
import os
import sys
import time
from datetime import datetime
from typing import Dict, List

# Add ETL modules to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.database import mongo_conn, clickhouse_conn
from extract.mongo_extractor import MongoExtractor
from transform.data_transformer import DataTransformer
from load.clickhouse_loader import ClickHouseLoader

class ETLPipeline:
    """Main ETL Pipeline orchestrator"""
    
    def __init__(self):
        self.extractor = MongoExtractor()
        self.transformer = DataTransformer()
        self.loader = ClickHouseLoader()
        self.start_time = None
        self.book_isbn_mapping = {}
    
    def run_full_etl(self):
        """Run complete ETL pipeline"""
        self.start_time = datetime.now()
        print("🚀 Starting MongoDB Atlas to ClickHouse ETL Pipeline")
        print(f"⏰ Started at: {self.start_time}")
        
        try:
            # Step 1: Connect to databases
            if not self._connect_databases():
                return False
            
            # Step 2: Extract data from MongoDB
            extracted_data = self._extract_data()
            if not extracted_data:
                print("❌ No data extracted. Exiting.")
                return False
            
            # Step 3: Create book_id to ISBN mapping
            self._create_book_mapping(extracted_data)
            
            # Step 4: Transform data
            transformed_data = self._transform_data(extracted_data)
            
            # Step 5: Setup ClickHouse schema
            self._setup_clickhouse_schema()
            
            # Step 6: Load data to ClickHouse
            self._load_data(transformed_data)
            
            # Step 7: Verify and cleanup
            self._verify_and_cleanup()
            
            # Success
            self._print_success_summary()
            return True
            
        except Exception as e:
            print(f"❌ ETL Pipeline failed: {e}")
            self._print_error_summary(e)
            return False
        
        finally:
            self._disconnect_databases()
    
    def _connect_databases(self) -> bool:
        """Connect to MongoDB and ClickHouse"""
        print("\n🔌 Connecting to databases...")
        
        # Connect to MongoDB Atlas
        if not mongo_conn.connect():
            return False
        
        # Connect to ClickHouse
        if not clickhouse_conn.connect():
            return False
        
        # Create ClickHouse database if not exists
        self.loader.create_database_if_not_exists()
        
        return True
    
    def _extract_data(self) -> Dict[str, List[Dict]]:
        """Extract data from MongoDB collections"""
        print("\n📥 Extracting data from MongoDB Atlas...")
        
        extracted_data = self.extractor.extract_all_collections()
        
        # Print extraction summary
        total_records = sum(len(data) for data in extracted_data.values())
        print(f"\n📊 Extraction Summary:")
        for table, data in extracted_data.items():
            print(f"  • {table}: {len(data):,} records")
        print(f"  📈 Total: {total_records:,} records")
        
        return extracted_data
    
    def _create_book_mapping(self, extracted_data: Dict[str, List[Dict]]):
        """Create mapping from book_id to ISBN for joins"""
        print("\n🗺️  Creating book_id to ISBN mapping...")
        
        if 'dim_book' in extracted_data:
            for book in extracted_data['dim_book']:
                book_id = book.get('id')
                isbn = book.get('isbn')
                if book_id and isbn:
                    self.book_isbn_mapping[str(book_id)] = isbn
        
        print(f"✅ Created mapping for {len(self.book_isbn_mapping)} books")
    
    def _transform_data(self, extracted_data: Dict[str, List[Dict]]) -> Dict[str, List[Dict]]:
        """Transform extracted data to ClickHouse schema"""
        print("\n🔄 Transforming data...")
        
        transformed_data = self.transformer.transform_all_data(
            extracted_data, 
            self.book_isbn_mapping
        )
        
        # Print transformation summary
        total_records = sum(len(data) for data in transformed_data.values())
        print(f"\n📊 Transformation Summary:")
        for table, data in transformed_data.items():
            print(f"  • {table}: {len(data):,} records")
        print(f"  📈 Total: {total_records:,} records")
        
        return transformed_data
    
    def _setup_clickhouse_schema(self):
        """Setup ClickHouse schema"""
        print("\n🏗️  Setting up ClickHouse schema...")
        
        # Read and execute schema file
        schema_file = os.path.join(os.path.dirname(__file__), 'clickhouse-init', 'library_dw.sql')
        
        if os.path.exists(schema_file):
            with open(schema_file, 'r') as f:
                schema_sql = f.read()
            
            # Split by semicolon and execute each statement
            statements = [stmt.strip() for stmt in schema_sql.split(';') if stmt.strip()]
            
            for statement in statements:
                try:
                    clickhouse_conn.execute_query(statement)
                except Exception as e:
                    # Ignore "already exists" errors
                    if "already exists" not in str(e).lower():
                        print(f"⚠️  Schema statement warning: {e}")
            
            print("✅ ClickHouse schema setup complete")
        else:
            print(f"⚠️  Schema file not found: {schema_file}")
        
        # Create date dimension (temporarily disabled due to data type issues)
        # self.loader.create_date_dimension()
        print("⚠️  Date dimension creation temporarily disabled")
    
    def _load_data(self, transformed_data: Dict[str, List[Dict]]):
        """Load transformed data to ClickHouse"""
        print("\n📤 Loading data to ClickHouse...")
        
        self.loader.load_all_tables(transformed_data)
    
    def _verify_and_cleanup(self):
        """Verify data load and cleanup"""
        print("\n🔍 Verifying data load...")
        
        self.loader.verify_data_load()
    
    def _disconnect_databases(self):
        """Disconnect from databases"""
        print("\n🔌 Disconnecting from databases...")
        
        mongo_conn.disconnect()
        clickhouse_conn.disconnect()
    
    def _print_success_summary(self):
        """Print success summary"""
        end_time = datetime.now()
        duration = end_time - self.start_time
        
        print(f"\n🎉 ETL Pipeline completed successfully!")
        print(f"⏰ Started:  {self.start_time}")
        print(f"⏰ Finished: {end_time}")
        print(f"⏱️  Duration: {duration}")
        print(f"📊 MongoDB Atlas → ClickHouse migration complete!")
    
    def _print_error_summary(self, error: Exception):
        """Print error summary"""
        end_time = datetime.now()
        duration = end_time - self.start_time if self.start_time else "Unknown"
        
        print(f"\n💥 ETL Pipeline failed!")
        print(f"⏰ Started:  {self.start_time}")
        print(f"⏰ Failed:   {end_time}")
        print(f"⏱️  Duration: {duration}")
        print(f"❌ Error: {error}")

def main():
    """Main entry point"""
    # Check if .env file exists
    if not os.path.exists('.env'):
        print("❌ .env file not found. Please copy .env.example to .env and configure your settings.")
        return
    
    # Run ETL pipeline
    pipeline = ETLPipeline()
    success = pipeline.run_full_etl()
    
    if success:
        print("\n✅ ETL Pipeline completed successfully!")
        sys.exit(0)
    else:
        print("\n❌ ETL Pipeline failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()
