#!/usr/bin/env python3
"""
Setup ClickHouse and run ETL pipeline
"""
import os
import sys
import requests
from dotenv import load_dotenv

def setup_clickhouse():
    """Setup ClickHouse tables"""
    print("🏗️ Setting up ClickHouse tables...")
    
    # Read setup SQL
    with open('setup_clickhouse.sql', 'r') as f:
        sql_statements = f.read().split(';')
    
    # Execute each statement
    for statement in sql_statements:
        statement = statement.strip()
        if statement:
            try:
                response = requests.post('http://localhost:8123', data=statement)
                if response.status_code == 200:
                    print(f"✅ Executed: {statement[:50]}...")
                else:
                    print(f"❌ Error: {response.text}")
            except Exception as e:
                print(f"❌ Error executing: {e}")
    
    print("✅ ClickHouse setup completed")

def run_etl():
    """Run ETL pipeline"""
    print("\n🚀 Running ETL pipeline...")
    os.system('python run_etl.py --verbose')

if __name__ == "__main__":
    load_dotenv()
    
    # Setup ClickHouse
    setup_clickhouse()
    
    # Run ETL
    run_etl()
