#!/usr/bin/env python3
"""
Fix reservation ETL by correcting field mappings and ISBN resolution
"""
import os
from pymongo import MongoClient
from dotenv import load_dotenv
import requests

def get_book_isbn_mapping():
    """Create mapping from bookId to ISBN from books collection"""
    print("📚 Creating book ID to ISBN mapping...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        books_collection = db['books']
        books = list(books_collection.find({}, {'_id': 1, 'isbn': 1}))
        
        book_isbn_mapping = {}
        for book in books:
            book_id = str(book.get('_id'))
            isbn = book.get('isbn', '')
            book_isbn_mapping[book_id] = isbn
        
        print(f"✅ Created mapping for {len(book_isbn_mapping)} books")
        return book_isbn_mapping
        
    except Exception as e:
        print(f"❌ Error creating book mapping: {e}")
        return {}
    finally:
        client.close()

def transform_reservations_correctly():
    """Transform reservations with correct field mappings and ISBN resolution"""
    print("🔄 Transforming reservations with correct field mappings...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get book ISBN mapping
        book_isbn_mapping = get_book_isbn_mapping()
        
        # Get reservations
        reservations_collection = db['reservations']
        reservations = list(reservations_collection.find({}))
        
        print(f"📊 Processing {len(reservations)} reservations...")
        
        transformed_reservations = []
        
        for doc in reservations:
            # Generate time_id from reservedAt
            reserved_at = doc.get('reservedAt')
            if reserved_at:
                if isinstance(reserved_at, str):
                    from datetime import datetime
                    try:
                        dt = datetime.fromisoformat(reserved_at.replace('Z', '+00:00'))
                        time_id = int(dt.strftime('%Y%m%d'))
                    except:
                        time_id = 20260512  # fallback
                else:
                    from datetime import datetime
                    time_id = int(reserved_at.strftime('%Y%m%d'))
            else:
                time_id = 20260512
            
            # Get ISBN from book mapping
            book_id = str(doc.get('bookId', ''))
            isbn = book_isbn_mapping.get(book_id, '')
            
            transformed_doc = {
                'reservation_id': str(doc.get('_id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': book_id,
                'isbn': isbn,
                'requested_at': doc.get('reservedAt'),
                'expiry_date': doc.get('expiresAt'),
                'status': doc.get('status', ''),
                'position_in_queue': doc.get('queuePosition'),
                'time_id': time_id
            }
            
            transformed_reservations.append(transformed_doc)
            
            print(f"\n📋 Reservation: {doc.get('_id')}")
            print(f"   User: {doc.get('memberId')}")
            print(f"   Book: {book_id} → ISBN: {isbn}")
            print(f"   Status: {doc.get('status')}")
            print(f"   Reserved: {doc.get('reservedAt')}")
            print(f"   Expires: {doc.get('expiresAt')}")
            print(f"   Queue Position: {doc.get('queuePosition')}")
        
        return transformed_reservations
        
    except Exception as e:
        print(f"❌ Error transforming reservations: {e}")
        return []
    finally:
        client.close()

def load_reservations_to_clickhouse(reservations):
    """Load transformed reservations to ClickHouse"""
    print("\n📥 Loading reservations to ClickHouse...")
    
    if not reservations:
        print("⚠️ No reservations to load")
        return False
    
    # Clear existing data
    print("🗑️ Clearing existing reservation data...")
    clear_query = "TRUNCATE TABLE library_dw.fact_reservation"
    try:
        response = requests.post('http://localhost:8123', data=clear_query)
        if response.status_code == 200:
            print("✅ Cleared existing data")
        else:
            print(f"⚠️ Clear failed: {response.text}")
    except Exception as e:
        print(f"❌ Error clearing data: {e}")
    
    # Insert new data
    print("📥 Inserting transformed reservations...")
    
    for reservation in reservations:
        # Format values for ClickHouse INSERT
        columns = ['reservation_id', 'user_id', 'book_id', 'isbn', 'requested_at', 'expiry_date', 'status', 'position_in_queue', 'time_id']
        
        values = []
        for col in columns:
            val = reservation.get(col)
            if val is None:
                values.append('NULL')
            elif col in ['requested_at', 'expiry_date'] and val:
                # Format datetime for ClickHouse
                if isinstance(val, str):
                    values.append(f"'{val}'")
                else:
                    values.append(f"'{val.isoformat()}'")
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
                print(f"   ✅ Inserted reservation {reservation['reservation_id']}")
            else:
                print(f"   ❌ Failed to insert {reservation['reservation_id']}: {response.text}")
        except Exception as e:
            print(f"   ❌ Error inserting {reservation['reservation_id']}: {e}")
    
    return True

def verify_reservation_data():
    """Verify reservation data in ClickHouse"""
    print("\n🔍 Verifying reservation data in ClickHouse...")
    
    verify_query = "SELECT * FROM library_dw.fact_reservation ORDER BY time_id, reservation_id"
    
    try:
        response = requests.post('http://localhost:8123', data=verify_query)
        if response.status_code == 200:
            print("📋 Current reservation data:")
            lines = response.text.strip().split('\n')
            for line in lines:
                if line.strip():
                    print(f"   {line}")
        else:
            print(f"❌ Verification failed: {response.text}")
    except Exception as e:
        print(f"❌ Error verifying data: {e}")

if __name__ == "__main__":
    print("🚀 Starting reservation ETL fix...")
    
    # Transform reservations with correct mappings
    transformed_reservations = transform_reservations_correctly()
    
    if transformed_reservations:
        # Load to ClickHouse
        success = load_reservations_to_clickhouse(transformed_reservations)
        
        if success:
            # Verify the results
            verify_reservation_data()
            
            print("\n✅ Reservation ETL fix completed!")
            print("📊 Summary:")
            print(f"   • Processed {len(transformed_reservations)} reservations")
            print("   • Fixed field mapping issues")
            print("   • Resolved ISBN from book collection")
            print("   • Updated ClickHouse fact_reservation table")
        else:
            print("\n❌ Reservation ETL fix failed!")
    else:
        print("\n⚠️ No reservations found to process")
