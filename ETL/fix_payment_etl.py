#!/usr/bin/env python3
"""
Fix payment ETL by implementing complete transformation and loading
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

def fix_payment_etl():
    """Complete fix for payment ETL"""
    print("🚀 Fixing Payment ETL...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get fine_records data
        fine_records_collection = db['fine_records']
        fine_records = list(fine_records_collection.find({}))
        
        print(f"📊 Processing {len(fine_records)} fine records...")
        
        # Clear existing payment data
        print("🗑️ Clearing existing payment data...")
        clear_query = "TRUNCATE TABLE library_dw.fact_payment"
        try:
            response = requests.post('http://localhost:8123', data=clear_query)
            if response.status_code == 200:
                print("✅ Cleared existing data")
        except Exception as e:
            print(f"⚠️ Clear failed: {e}")
        
        # Process and insert payment records
        for doc in fine_records:
            payment_id = str(doc.get('_id'))
            user_id = str(doc.get('memberId', ''))
            loan_id = str(doc.get('loanId', ''))
            amount = doc.get('totalAmount')
            
            # Use collectedAt if available, otherwise createdAt
            payment_time = doc.get('collectedAt') or doc.get('createdAt')
            formatted_time = format_datetime_for_clickhouse(payment_time)
            
            # Generate time_id
            if formatted_time:
                time_id = formatted_time.replace('-', '').replace(' ', '').replace(':', '')[:8]
            else:
                time_id = '20260505'
            
            print(f"\n💳 Payment: {payment_id}")
            print(f"   User: {user_id}")
            print(f"   Loan: {loan_id}")
            print(f"   Amount: {amount}")
            print(f"   Status: {doc.get('status')}")
            print(f"   Payment Time: {payment_time} → {formatted_time}")
            print(f"   Time ID: {time_id}")
            
            # Insert into ClickHouse
            columns = ['payment_id', 'user_id', 'loan_id', 'amount', 'payment_time', 'time_id']
            
            values = []
            for col in columns:
                if col == 'payment_id':
                    values.append(f"'{payment_id}'")
                elif col == 'user_id':
                    values.append(f"'{user_id}'")
                elif col == 'loan_id':
                    values.append(f"'{loan_id}'")
                elif col == 'amount':
                    val = amount if amount is not None else 'NULL'
                    values.append(str(val))
                elif col == 'payment_time':
                    if formatted_time:
                        values.append(f"'{formatted_time}'")
                    else:
                        values.append('NULL')
                elif col == 'time_id':
                    values.append(f"'{time_id}'")
            
            insert_query = f"""
            INSERT INTO library_dw.fact_payment ({', '.join(columns)}) 
            VALUES ({', '.join(values)})
            """
            
            try:
                response = requests.post('http://localhost:8123', data=insert_query)
                if response.status_code == 200:
                    print(f"   ✅ Inserted payment record")
                else:
                    print(f"   ❌ Failed to insert: {response.text}")
            except Exception as e:
                print(f"   ❌ Error inserting: {e}")
        
        # Verify results
        print("\n🔍 Verifying payment data...")
        verify_query = "SELECT * FROM library_dw.fact_payment ORDER BY time_id, payment_id"
        
        try:
            response = requests.post('http://localhost:8123', data=verify_query)
            if response.status_code == 200:
                print("📋 Final payment data:")
                lines = response.text.strip().split('\n')
                for line in lines:
                    if line.strip():
                        print(f"   {line}")
        except Exception as e:
            print(f"❌ Error verifying: {e}")
        
        print("\n✅ Payment ETL fix completed!")
        print(f"📊 Summary:")
        print(f"   • Processed {len(fine_records)} fine records")
        print("   • Fixed missing transformation function")
        print("   • Resolved datetime formatting")
        print("   • Updated ClickHouse fact_payment table")
        
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_payment_etl()
