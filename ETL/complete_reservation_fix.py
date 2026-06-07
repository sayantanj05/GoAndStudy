#!/usr/bin/env python3
"""
Complete fix for reservation ETL with proper datetime handling
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
            # Parse string to datetime
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
            # Already a datetime object
            dt = dt_obj
        else:
            return None
        
        # Format as ClickHouse compatible string
        return dt.strftime('%Y-%m-%d %H:%M:%S')
        
    except Exception as e:
        print(f"❌ Error formatting datetime {dt_obj}: {e}")
        return None

def complete_reservation_fix():
    """Complete fix for reservation data"""
    print("🚀 Complete reservation ETL fix...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get book ISBN mapping
        print("📚 Creating book ID to ISBN mapping...")
        books_collection = db['books']
        books = list(books_collection.find({}, {'_id': 1, 'isbn': 1}))
        
        book_isbn_mapping = {}
        for book in books:
            book_id = str(book.get('_id'))
            isbn = book.get('isbn', '')
            book_isbn_mapping[book_id] = isbn
        
        print(f"✅ Created mapping for {len(book_isbn_mapping)} books")
        
        # Get reservations
        reservations_collection = db['reservations']
        reservations = list(reservations_collection.find({}))
        
        print(f"📊 Processing {len(reservations)} reservations...")
        
        # Clear existing data
        print("🗑️ Clearing existing reservation data...")
        clear_query = "TRUNCATE TABLE library_dw.fact_reservation"
        try:
            response = requests.post('http://localhost:8123', data=clear_query)
            if response.status_code == 200:
                print("✅ Cleared existing data")
        except Exception as e:
            print(f"⚠️ Clear failed: {e}")
        
        # Process and insert reservations
        for doc in reservations:
            reservation_id = str(doc.get('_id'))
            
            # Get ISBN from book mapping
            book_id = str(doc.get('bookId', ''))
            isbn = book_isbn_mapping.get(book_id, '')
            
            # Format datetime fields
            requested_at = format_datetime_for_clickhouse(doc.get('reservedAt'))
            expiry_date = format_datetime_for_clickhouse(doc.get('expiresAt'))
            
            # Generate time_id
            if requested_at:
                time_id = requested_at.replace('-', '').replace(' ', '').replace(':', '')[:8]
            else:
                time_id = '20260505'
            
            print(f"\n📋 Reservation: {reservation_id}")
            print(f"   User: {doc.get('memberId')}")
            print(f"   Book: {book_id} → ISBN: {isbn}")
            print(f"   Status: {doc.get('status')}")
            print(f"   Reserved: {doc.get('reservedAt')} → {requested_at}")
            print(f"   Expires: {doc.get('expiresAt')} → {expiry_date}")
            print(f"   Queue Position: {doc.get('queuePosition')}")
            
            # Insert into ClickHouse
            columns = ['reservation_id', 'user_id', 'book_id', 'isbn', 'requested_at', 'expiry_date', 'status', 'position_in_queue', 'time_id']
            
            # Handle NULL values properly
            values = []
            for col in columns:
                val = locals().get(col.replace('position_in_queue', 'queuePosition').replace('user_id', 'memberId').replace('book_id', 'bookId'))
                if col == 'reservation_id':
                    val = reservation_id
                elif col == 'user_id':
                    val = str(doc.get('memberId', ''))
                elif col == 'book_id':
                    val = book_id
                elif col == 'isbn':
                    val = isbn
                elif col == 'requested_at':
                    val = requested_at
                elif col == 'expiry_date':
                    val = expiry_date
                elif col == 'status':
                    val = doc.get('status', '')
                elif col == 'position_in_queue':
                    val = doc.get('queuePosition')
                elif col == 'time_id':
                    val = time_id
                
                if val is None:
                    values.append('NULL')
                elif col in ['requested_at', 'expiry_date'] and val:
                    values.append(f"'{val}'")
                elif col in ['status', 'isbn', 'reservation_id', 'user_id', 'book_id']:
                    values.append(f"'{val}'")
                else:
                    values.append(str(val))
            
            insert_query = f"""
            INSERT INTO library_dw.fact_reservation ({', '.join(columns)}) 
            VALUES ({', '.join(values)})
            """
            
            try:
                response = requests.post('http://localhost:8123', data=insert_query)
                if response.status_code == 200:
                    print(f"   ✅ Inserted reservation")
                else:
                    print(f"   ❌ Failed to insert: {response.text}")
            except Exception as e:
                print(f"   ❌ Error inserting: {e}")
        
        # Verify results
        print("\n🔍 Verifying final results...")
        verify_query = "SELECT * FROM library_dw.fact_reservation ORDER BY time_id, reservation_id"
        
        try:
            response = requests.post('http://localhost:8123', data=verify_query)
            if response.status_code == 200:
                print("📋 Final reservation data:")
                lines = response.text.strip().split('\n')
                for line in lines:
                    if line.strip():
                        print(f"   {line}")
        except Exception as e:
            print(f"❌ Error verifying: {e}")
        
        print("\n✅ Complete reservation fix finished!")
        
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    complete_reservation_fix()
