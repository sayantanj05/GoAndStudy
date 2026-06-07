#!/usr/bin/env python3
"""
Verify and fix the age and gender updates in dim_user table
"""
import os
import requests
from pymongo import MongoClient
from dotenv import load_dotenv
from datetime import datetime, date

def calculate_real_time_age(date_of_birth):
    """Calculate age in years from date of birth to current date"""
    if not date_of_birth:
        return None
    
    try:
        if isinstance(date_of_birth, str):
            for fmt in ['%Y-%m-%d %H:%M:%S', '%Y-%m-%d', '%Y-%m-%d %H:%M:%S.%f']:
                try:
                    birth_date = datetime.strptime(date_of_birth, fmt).date()
                    break
                except ValueError:
                    continue
            else:
                return None
        elif hasattr(date_of_birth, 'date'):
            birth_date = date_of_birth.date()
        elif isinstance(date_of_birth, datetime):
            birth_date = date_of_birth.date()
        else:
            return None
        
        today = date.today()
        age = today.year - birth_date.year - ((today.month, today.day) < (birth_date.month, birth_date.day))
        return age
    except Exception:
        return None

def execute_clickhouse_query(query):
    """Execute ClickHouse query via HTTP"""
    try:
        response = requests.post('http://localhost:8123', data=query)
        if response.status_code == 200:
            return response.text.strip()
        else:
            print(f"❌ Query failed: {response.text}")
            return None
    except Exception as e:
        print(f"❌ Connection error: {e}")
        return None

def verify_and_fix():
    """Verify current state and fix updates"""
    print("🔍 Verifying current dim_user table state...")
    
    load_dotenv()
    
    # Check current data
    current_query = "SELECT member_id, name, age, gender FROM library_dw.dim_user ORDER BY member_id"
    current_data = execute_clickhouse_query(current_query)
    
    if current_data:
        print("📋 Current data in dim_user:")
        lines = current_data.split('\n')
        for line in lines:
            if line.strip():
                print(f"   {line}")
    
    # Get fresh data from MongoDB
    print("\n📥 Getting fresh data from MongoDB...")
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    try:
        mongo_client = MongoClient(mongo_uri)
        mongo_db = mongo_client[mongo_db_name]
        members_collection = mongo_db['members']
        members = list(members_collection.find({}))
        
        print(f"\n🔄 Updating {len(members)} members...")
        
        for member in members:
            member_id = member.get('_id')
            name = member.get('name', 'N/A')
            date_of_birth = member.get('dateOfBirth')
            gender = member.get('gender')
            
            age = calculate_real_time_age(date_of_birth)
            
            print(f"\n👤 {member_id} - {name}")
            print(f"   DOB: {date_of_birth}")
            print(f"   Age: {age}")
            print(f"   Gender: {gender}")
            
            # Fix the UPDATE query with proper NULL handling
            age_str = str(age) if age is not None else 'NULL'
            gender_str = f"'{gender}'" if gender else 'NULL'
            
            update_query = f"""
            ALTER TABLE library_dw.dim_user 
            UPDATE age = {age_str}, gender = {gender_str} 
            WHERE member_id = '{member_id}'
            """
            
            result = execute_clickhouse_query(update_query)
            if result is not None:
                print(f"   ✅ Update successful")
            else:
                print(f"   ❌ Update failed")
        
        # Final verification
        print("\n🔍 Final verification...")
        final_query = "SELECT member_id, name, age, gender FROM library_dw.dim_user ORDER BY member_id"
        final_data = execute_clickhouse_query(final_query)
        
        if final_data:
            print("📋 Final data in dim_user:")
            lines = final_data.split('\n')
            for line in lines:
                if line.strip():
                    print(f"   {line}")
        
        mongo_client.close()
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    verify_and_fix()
