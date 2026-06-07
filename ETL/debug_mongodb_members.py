#!/usr/bin/env python3
"""
Debug MongoDB members collection
"""
from pymongo import MongoClient
import os
from dotenv import load_dotenv

def debug_mongodb_members():
    """Debug MongoDB members collection"""
    print("🔍 Debugging MongoDB members collection...")
    
    load_dotenv()
    
    # Connect to MongoDB
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Check collection exists
        collections = db.list_collection_names()
        print(f"📋 Collections: {collections}")
        
        if 'members' in collections:
            collection = db['members']
            
            # Get total count
            total_docs = collection.count_documents({})
            print(f"📊 Total documents in members: {total_docs}")
            
            # Get sample documents
            print("\n📋 Sample documents:")
            for i, doc in enumerate(collection.find().limit(3)):
                print(f"\n--- Document {i+1} ---")
                for key, value in doc.items():
                    print(f"{key}: {value}")
                print("--- End Document ---")
        else:
            print("❌ 'members' collection not found")
            
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    debug_mongodb_members()
