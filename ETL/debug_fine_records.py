#!/usr/bin/env python3
"""
Debug MongoDB fine_records collection for payment data
"""
from pymongo import MongoClient
import os
from dotenv import load_dotenv

def debug_fine_records():
    """Debug MongoDB fine_records collection"""
    print("🔍 Debugging MongoDB fine_records collection...")
    
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
        
        if 'fine_records' in collections:
            collection = db['fine_records']
            
            # Get total count
            total_docs = collection.count_documents({})
            print(f"📊 Total documents in fine_records: {total_docs}")
            
            if total_docs > 0:
                # Get sample documents
                print("\n📋 Sample fine_records documents:")
                for i, doc in enumerate(collection.find().limit(5)):
                    print(f"\n--- Document {i+1} ---")
                    for key, value in doc.items():
                        print(f"{key}: {value}")
                    print("--- End Document ---")
            else:
                print("⚠️ No fine_records documents found")
        else:
            print("❌ 'fine_records' collection not found")
            
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    debug_fine_records()
