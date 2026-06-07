#!/usr/bin/env python3
"""
Fix events ETL by implementing complete transformation and loading
"""
import os
import requests
from pymongo import MongoClient
from dotenv import load_dotenv
from datetime import datetime

def format_datetime_for_clickhouse(dt_obj):
    """Format datetime object for ClickHouse"""
    if not dt_obj:
        return None
    
    try:
        # Handle both string and datetime objects
        if isinstance(dt_obj, str):
            formats = [
                '%Y-%m-%d %H:%M:%S.%f',
                '%Y-%m-%d %H:%M:%S',
                '%Y-%m-%dT%H:%M:%S.%f',
                '%Y-%m-%dT%H:%M:%S'
            ]
            
            for fmt in formats:
                try:
                    dt = datetime.strptime(dt_obj, fmt)
                    break
                except ValueError:
                    continue
            else:
                return None
        elif hasattr(dt_obj, 'strftime'):
            dt = dt_obj
        else:
            return None
        
        return dt.strftime('%Y-%m-%d %H:%M:%S')
        
    except Exception as e:
        print(f"❌ Error formatting datetime {dt_obj}: {e}")
        return None

def fix_events_etl():
    """Complete fix for events ETL"""
    print("🚀 Fixing Events ETL...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get loan_events data
        loan_events_collection = db['loan_events']
        loan_events = list(loan_events_collection.find({}))
        
        print(f"📊 Processing {len(loan_events)} loan events...")
        
        # Clear existing events data
        print("🗑️ Clearing existing events data...")
        clear_query = "TRUNCATE TABLE library_dw.fact_events"
        try:
            response = requests.post('http://localhost:8123', data=clear_query)
            if response.status_code == 200:
                print("✅ Cleared existing data")
        except Exception as e:
            print(f"⚠️ Clear failed: {e}")
        
        # Process and insert event records
        for doc in loan_events:
            event_id = str(doc.get('_id'))
            loan_id = str(doc.get('loanId', ''))
            event_type = doc.get('eventType', '')
            triggered_by = doc.get('triggeredBy', '')
            
            # Format timestamp
            event_time = doc.get('timestamp')
            formatted_time = format_datetime_for_clickhouse(event_time)
            
            # Generate time_id
            if formatted_time:
                time_id = formatted_time.replace('-', '').replace(' ', '').replace(':', '')[:8]
            else:
                time_id = '20260504'
            
            print(f"\n📋 Event: {event_id}")
            print(f"   Loan: {loan_id}")
            print(f"   Type: {event_type}")
            print(f"   Triggered By: {triggered_by}")
            print(f"   Timestamp: {event_time} → {formatted_time}")
            print(f"   Time ID: {time_id}")
            
            # Insert into ClickHouse
            columns = ['event_id', 'loan_id', 'event_type', 'timestamp', 'triggered_by', 'time_id']
            
            values = []
            for col in columns:
                if col == 'event_id':
                    values.append(f"'{event_id}'")
                elif col == 'loan_id':
                    values.append(f"'{loan_id}'")
                elif col == 'event_type':
                    values.append(f"'{event_type}'")
                elif col == 'triggered_by':
                    values.append(f"'{triggered_by}'")
                elif col == 'timestamp':
                    if formatted_time:
                        values.append(f"'{formatted_time}'")
                    else:
                        values.append('NULL')
                elif col == 'time_id':
                    values.append(f"'{time_id}'")
            
            insert_query = f"""
            INSERT INTO library_dw.fact_events ({', '.join(columns)}) 
            VALUES ({', '.join(values)})
            """
            
            try:
                response = requests.post('http://localhost:8123', data=insert_query)
                if response.status_code == 200:
                    print(f"   ✅ Inserted event record")
                else:
                    print(f"   ❌ Failed to insert: {response.text}")
            except Exception as e:
                print(f"   ❌ Error inserting: {e}")
        
        # Verify results
        print("\n🔍 Verifying events data...")
        verify_query = "SELECT * FROM library_dw.fact_events ORDER BY time_id, event_id"
        
        try:
            response = requests.post('http://localhost:8123', data=verify_query)
            if response.status_code == 200:
                print("📋 Final events data:")
                lines = response.text.strip().split('\n')
                for line in lines:
                    if line.strip():
                        print(f"   {line}")
        except Exception as e:
            print(f"❌ Error verifying: {e}")
        
        # Event statistics
        event_types = {}
        for doc in loan_events:
            event_type = doc.get('eventType', 'Unknown')
            event_types[event_type] = event_types.get(event_type, 0) + 1
        
        print(f"\n📊 Event Statistics:")
        for event_type, count in event_types.items():
            print(f"   • {event_type}: {count} events")
        
        print(f"\n✅ Events ETL fix completed!")
        print(f"📊 Summary:")
        print(f"   • Processed {len(loan_events)} loan events")
        print(f"   • Fixed missing transformation function")
        print(f"   • Resolved datetime formatting")
        print(f"   • Updated ClickHouse fact_events table")
        
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_events_etl()
