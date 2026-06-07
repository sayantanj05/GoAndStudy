#!/usr/bin/env python3
"""
Check ClickHouse table structure
"""
import requests

def check_table_structure():
    """Check structure of ClickHouse tables"""
    print("🔍 Checking ClickHouse table structures...")
    
    tables_to_check = [
        'dim_user',
        'dim_book', 
        'fact_loan',
        'fact_payment',
        'fact_ratings',
        'fact_wishlist',
        'fact_reading_session',
        'fact_reservation',
        'fact_search',
        'fact_activity_log'
    ]
    
    for table in tables_to_check:
        print(f"\n📋 Table: {table}")
        
        # Get table structure
        query = f"DESCRIBE library_dw.{table}"
        try:
            response = requests.post('http://localhost:8123', data=query)
            if response.status_code == 200:
                print(response.text)
            else:
                print(f"❌ Error: {response.text}")
        except Exception as e:
            print(f"❌ Error: {e}")

if __name__ == "__main__":
    check_table_structure()
