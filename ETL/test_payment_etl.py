#!/usr/bin/env python3
"""
Test payment ETL transformation and loading
"""
from extract.mongo_extractor import MongoExtractor
from transform.data_transformer import DataTransformer
from load.clickhouse_loader import ClickHouseLoader
from dotenv import load_dotenv
import os

def test_payment_etl():
    """Test payment ETL end-to-end"""
    print("🚀 Testing Payment ETL...")
    
    load_dotenv()
    
    try:
        # Extract
        extractor = MongoExtractor()
        payment_data = extractor.extract_collection('fine_records')
        print(f"📥 Extracted {len(payment_data)} payment records")
        
        # Transform
        transformer = DataTransformer()
        transformed_payments = transformer.transform_fact_payment(payment_data)
        print(f"🔄 Transformed {len(transformed_payments)} payment records")
        
        # Load
        loader = ClickHouseLoader()
        loader.insert_data('fact_payment', transformed_payments, 
                         ['payment_id', 'user_id', 'loan_id', 'amount', 'payment_time', 'time_id'])
        
        print("✅ Payment ETL completed!")
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    test_payment_etl()
