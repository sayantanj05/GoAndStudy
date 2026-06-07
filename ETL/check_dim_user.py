#!/usr/bin/env python3
"""
Check dim_user table data
"""
import requests

def check_dim_user_data():
    """Check dim_user table data"""
    print("🔍 Checking dim_user table data...")
    
    # Check total count
    query = "SELECT COUNT(*) as total_records FROM library_dw.dim_user"
    try:
        response = requests.post('http://localhost:8123', data=query)
        if response.status_code == 200:
            print(f"📊 Total records: {response.text.strip()}")
    except Exception as e:
        print(f"❌ Error: {e}")
    
    # Check sample data
    query = "SELECT * FROM library_dw.dim_user LIMIT 10"
    try:
        response = requests.post('http://localhost:8123', data=query)
        if response.status_code == 200:
            print("\n📋 Sample data:")
            print(response.text)
    except Exception as e:
        print(f"❌ Error: {e}")
    
    # Check member_id specifically
    query = "SELECT member_id, COUNT(*) FROM library_dw.dim_user GROUP BY member_id"
    try:
        response = requests.post('http://localhost:8123', data=query)
        if response.status_code == 200:
            print("\n🔍 member_id analysis:")
            print(response.text)
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    check_dim_user_data()
