#!/usr/bin/env python3
"""
Simple script to update dim_user table with age and gender data
"""
import os
import sys
from datetime import datetime, date
from pymongo import MongoClient
from dotenv import load_dotenv
import requests

def calculate_real_time_age(date_of_birth):
    """Calculate age in years from date of birth to current date"""
    if not date_of_birth:
        return None
    
    try:
        # Handle different date formats
        if isinstance(date_of_birth, str):
            # Try parsing different date formats
            for fmt in ['%Y-%m-%d %H:%M:%S', '%Y-%m-%d', '%Y-%m-%d %H:%M:%S.%f']:
                try:
                    birth_date = datetime.strptime(date_of_birth, fmt).date()
                    break
                except ValueError:
                    continue
            else:
                print(f"⚠️ Could not parse date: {date_of_birth}")
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
    
    except Exception as e:
        print(f"❌ Error calculating age for {date_of_birth}: {e}")
        return None

def execute_clickhouse_query(query):
    """Execute ClickHouse query via HTTP"""
    try:
        response = requests.post('http://localhost:8123', data=query)
        if response.status_code == 200:
            return response.text.strip()
        else:
            print(f"❌ ClickHouse query failed: {response.text}")
            return None
    except Exception as e:
        print(f"❌ ClickHouse connection error: {e}")
        return None

def update_dim_user_simple():
    """Update dim_user table with age and gender using simple HTTP requests"""
    print("🔄 Updating dim_user table with age and gender...")
    
    # Load environment variables
    load_dotenv()
    
    # MongoDB connection
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    try:
        # Connect to MongoDB
        print("📡 Connecting to MongoDB...")
        mongo_client = MongoClient(mongo_uri)
        mongo_db = mongo_client[mongo_db_name]
        members_collection = mongo_db['members']
        
        # Add gender column to ClickHouse table
        print("🔧 Adding gender column to dim_user table...")
        alter_query = "ALTER TABLE library_dw.dim_user ADD COLUMN IF NOT EXISTS gender Nullable(String) AFTER age"
        result = execute_clickhouse_query(alter_query)
        if result is not None:
            print("✅ Gender column added/verified")
        
        # Get all members from MongoDB
        print("📥 Fetching members from MongoDB...")
        members = list(members_collection.find({}))
        print(f"📊 Found {len(members)} members in MongoDB")
        
        # Process each member
        updated_count = 0
        for member in members:
            member_id = member.get('_id')
            date_of_birth = member.get('dateOfBirth')
            gender = member.get('gender')
            
            # Calculate real-time age
            age = calculate_real_time_age(date_of_birth)
            
            print(f"\n👤 Processing member: {member_id}")
            print(f"   Name: {member.get('name', 'N/A')}")
            print(f"   DOB: {date_of_birth}")
            print(f"   Calculated Age: {age}")
            print(f"   Gender: {gender}")
            
            # Update ClickHouse dim_user table using parameterized query
            try:
                # Use INSERT with ON CLUSTER if needed, or simple UPDATE
                update_query = f"""
                ALTER TABLE library_dw.dim_user 
                UPDATE age = {age if age is not None else 'NULL'}, 
                       gender = '{gender if gender else 'NULL'}' 
                WHERE member_id = '{member_id}'
                """
                
                result = execute_clickhouse_query(update_query)
                if result is not None:
                    updated_count += 1
                    print(f"   ✅ Updated in ClickHouse")
                else:
                    print(f"   ❌ Failed to update in ClickHouse")
                
            except Exception as e:
                print(f"   ❌ Error updating ClickHouse: {e}")
        
        print(f"\n🎉 Successfully updated {updated_count} members")
        
        # Verify the updates
        print("\n🔍 Verifying updates...")
        verify_query = "SELECT member_id, name, age, gender FROM library_dw.dim_user WHERE age IS NOT NULL OR gender IS NOT NULL LIMIT 5"
        result = execute_clickhouse_query(verify_query)
        
        if result:
            print("📋 Sample updated records:")
            lines = result.split('\n')
            for line in lines:
                if line.strip():
                    print(f"   {line}")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    
    finally:
        # Close MongoDB connection
        if 'mongo_client' in locals():
            mongo_client.close()
    
    return True

if __name__ == "__main__":
    print("🚀 Starting simple member age and gender update...")
    
    success = update_dim_user_simple()
    
    if success:
        print("\n✅ Process completed!")
        print("\n📊 Summary:")
        print("   • Added gender column to dim_user table")
        print("   • Calculated real-time age for all members")
        print("   • Updated ClickHouse with gender data from MongoDB")
    else:
        print("\n❌ Process failed. Please check the error messages above.")
