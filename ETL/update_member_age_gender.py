#!/usr/bin/env python3
"""
Update dim_user table with real-time age calculations and gender data from MongoDB
"""
import os
import sys
from datetime import datetime, date
from pymongo import MongoClient
from dotenv import load_dotenv
import clickhouse_connect

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

def update_dim_user_with_age_gender():
    """Update dim_user table with real-time age and gender data"""
    print("🔄 Updating dim_user table with real-time age and gender...")
    
    # Load environment variables
    load_dotenv()
    
    # MongoDB connection
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    # ClickHouse connection
    ch_host = os.getenv('CLICKHOUSE_HOST', 'localhost')
    ch_port = int(os.getenv('CLICKHOUSE_PORT', 8123))
    ch_user = os.getenv('CLICKHOUSE_USER', 'default')
    ch_password = os.getenv('CLICKHOUSE_PASSWORD', '')
    ch_database = os.getenv('CLICKHOUSE_DB', 'library_dw')
    
    try:
        # Connect to MongoDB
        print("📡 Connecting to MongoDB...")
        mongo_client = MongoClient(mongo_uri)
        mongo_db = mongo_client[mongo_db_name]
        members_collection = mongo_db['members']
        
        # Connect to ClickHouse
        print("📡 Connecting to ClickHouse...")
        ch_client = clickhouse_connect.get_client(
            host=ch_host,
            port=ch_port,
            username=ch_user,
            password=ch_password,
            database=ch_database
        )
        
        # First, add gender column if it doesn't exist
        print("🔧 Adding gender column to dim_user table...")
        try:
            ch_client.command('ALTER TABLE library_dw.dim_user ADD COLUMN IF NOT EXISTS gender Nullable(String) AFTER age')
            print("✅ Gender column added/verified")
        except Exception as e:
            print(f"⚠️ Gender column might already exist: {e}")
        
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
            
            # Update ClickHouse dim_user table
            try:
                update_query = """
                ALTER TABLE library_dw.dim_user 
                UPDATE age = ?, gender = ? 
                WHERE member_id = ?
                """
                
                ch_client.command(update_query, (age, gender, member_id))
                updated_count += 1
                print(f"   ✅ Updated in ClickHouse")
                
            except Exception as e:
                print(f"   ❌ Error updating ClickHouse: {e}")
        
        print(f"\n🎉 Successfully updated {updated_count} members")
        
        # Verify the updates
        print("\n🔍 Verifying updates...")
        result = ch_client.query('SELECT member_id, name, age, gender FROM library_dw.dim_user WHERE age IS NOT NULL OR gender IS NOT NULL LIMIT 5')
        
        print("📋 Sample updated records:")
        for row in result.result_rows:
            print(f"   ID: {row[0]}, Name: {row[1]}, Age: {row[2]}, Gender: {row[3]}")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    
    finally:
        # Close connections
        if 'mongo_client' in locals():
            mongo_client.close()
        if 'ch_client' in locals():
            ch_client.close()
    
    return True

def create_age_calculation_function():
    """Create a ClickHouse function for real-time age calculation"""
    print("🔧 Creating ClickHouse function for real-time age calculation...")
    
    load_dotenv()
    
    ch_host = os.getenv('CLICKHOUSE_HOST', 'localhost')
    ch_port = int(os.getenv('CLICKHOUSE_PORT', 8123))
    ch_user = os.getenv('CLICKHOUSE_USER', 'default')
    ch_password = os.getenv('CLICKHOUSE_PASSWORD', '')
    ch_database = os.getenv('CLICKHOUSE_DB', 'library_dw')
    
    try:
        ch_client = clickhouse_connect.get_client(
            host=ch_port,
            username=ch_user,
            password=ch_password,
            database=ch_database
        )
        
        # Create a function to calculate age from date_of_birth
        create_function_query = """
        CREATE OR REPLACE FUNCTION calculateAge AS (dob) -> 
            if(dob IS NULL, NULL, 
                dateDiff('year', dob, today()) - 
                if(dateDiff('month', dob, today()) < 0 OR 
                   (dateDiff('month', dob, today()) = 0 AND dateDiff('day', dob, today()) < 0), 1, 0)
            )
        """
        
        ch_client.command(create_function_query)
        print("✅ Age calculation function created in ClickHouse")
        
        # Test the function
        test_result = ch_client.query("SELECT calculateAge(toDate('2005-09-18')) as test_age")
        print(f"🧪 Test - Age for 2005-09-18: {test_result.result_rows[0][0]} years")
        
    except Exception as e:
        print(f"❌ Error creating function: {e}")

if __name__ == "__main__":
    print("🚀 Starting member age and gender update process...")
    
    # Create age calculation function in ClickHouse
    create_age_calculation_function()
    
    # Update dim_user table
    success = update_dim_user_with_age_gender()
    
    if success:
        print("\n✅ Process completed successfully!")
        print("\n📊 Summary:")
        print("   • Added gender column to dim_user table")
        print("   • Calculated real-time age for all members")
        print("   • Updated ClickHouse with gender data from MongoDB")
        print("   • Created reusable age calculation function")
    else:
        print("\n❌ Process failed. Please check the error messages above.")
