#!/usr/bin/env python3
"""
Fix datetime formatting for reservation data in ClickHouse
"""
import os
import requests
from pymongo import MongoClient
from dotenv import load_dotenv
from datetime import datetime

def format_datetime_for_clickhouse(dt_str):
    """Format datetime string for ClickHouse"""
    if not dt_str:
        return None
    
    try:
        # Parse the datetime string from MongoDB
        dt = datetime.fromisoformat(dt_str.replace('Z', '+00:00'))
        # Format as ClickHouse compatible string
        return dt.strftime('%Y-%m-%d %H:%M:%S')
    except Exception as e:
        print(f"❌ Error formatting datetime {dt_str}: {e}")
        return None

def fix_reservation_datetime():
    """Fix datetime fields in fact_reservation table"""
    print("🔧 Fixing datetime fields in fact_reservation...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get reservations from MongoDB
        reservations_collection = db['reservations']
        reservations = list(reservations_collection.find({}))
        
        print(f"📊 Processing {len(reservations)} reservations...")
        
        for reservation in reservations:
            reservation_id = str(reservation.get('_id'))
            
            # Format datetime fields
            requested_at = format_datetime_for_clickhouse(reservation.get('reservedAt'))
            expiry_date = format_datetime_for_clickhouse(reservation.get('expiresAt'))
            
            print(f"\n📋 Reservation: {reservation_id}")
            print(f"   Original requested_at: {reservation.get('reservedAt')}")
            print(f"   Formatted requested_at: {requested_at}")
            print(f"   Original expiry_date: {reservation.get('expiresAt')}")
            print(f"   Formatted expiry_date: {expiry_date}")
            
            # Update ClickHouse with properly formatted datetime
            if requested_at and expiry_date:
                update_query = f"""
                ALTER TABLE library_dw.fact_reservation 
                UPDATE requested_at = '{requested_at}', 
                       expiry_date = '{expiry_date}'
                WHERE reservation_id = '{reservation_id}'
                """
                
                try:
                    response = requests.post('http://localhost:8123', data=update_query)
                    if response.status_code == 200:
                        print(f"   ✅ Updated datetime fields")
                    else:
                        print(f"   ❌ Update failed: {response.text}")
                except Exception as e:
                    print(f"   ❌ Error updating: {e}")
            else:
                print(f"   ⚠️ Skipping due to datetime formatting issues")
        
        # Verify the fix
        print("\n🔍 Verifying datetime fix...")
        verify_query = "SELECT reservation_id, requested_at, expiry_date FROM library_dw.fact_reservation"
        
        try:
            response = requests.post('http://localhost:8123', data=verify_query)
            if response.status_code == 200:
                print("📋 Updated reservation data:")
                lines = response.text.strip().split('\n')
                for line in lines:
                    if line.strip():
                        print(f"   {line}")
        except Exception as e:
            print(f"❌ Error verifying: {e}")
        
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_reservation_datetime()
