#!/usr/bin/env python3
"""
Debug MongoDB reservations collection
"""
from pymongo import MongoClient
import os
from dotenv import load_dotenv

def debug_reservations():
    """Debug MongoDB reservations collection"""
    print("🔍 Debugging MongoDB reservations collection...")
    
    load_dotenv()
    
    # Connect to MongoDB
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Check collection exists
        collections = db.list_collection_names()
        print(f"📋 Available collections: {collections}")
        
        if 'reservations' in collections:
            collection = db['reservations']
            
            # Get total count
            total_docs = collection.count_documents({})
            print(f"📊 Total documents in reservations: {total_docs}")
            
            if total_docs > 0:
                # Get sample documents
                print("\n📋 Sample reservation documents:")
                for i, doc in enumerate(collection.find().limit(5)):
                    print(f"\n--- Document {i+1} ---")
                    for key, value in doc.items():
                        print(f"{key}: {value}")
                    print("--- End Document ---")
            else:
                print("⚠️ No reservation documents found")
        else:
            print("❌ 'reservations' collection not found")
            
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    debug_reservations()
