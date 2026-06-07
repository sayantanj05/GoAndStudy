#!/usr/bin/env python3
"""
Debug MongoDB collections to find event-related data
"""
from pymongo import MongoClient
import os
from dotenv import load_dotenv

def debug_event_collections():
    """Debug MongoDB for event-related collections"""
    print("🔍 Debugging MongoDB for event-related collections...")
    
    load_dotenv()
    
    # Connect to MongoDB
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Check all collections
        collections = db.list_collection_names()
        print(f"📋 Available collections: {collections}")
        
        # Look for event-related collections
        event_collections = [c for c in collections if 'event' in c.lower()]
        print(f"\n🎯 Event-related collections: {event_collections}")
        
        # Check loan_events specifically
        if 'loan_events' in collections:
            collection = db['loan_events']
            
            # Get total count
            total_docs = collection.count_documents({})
            print(f"\n📊 Total documents in loan_events: {total_docs}")
            
            if total_docs > 0:
                # Get sample documents
                print("\n📋 Sample loan_events documents:")
                for i, doc in enumerate(collection.find().limit(3)):
                    print(f"\n--- Document {i+1} ---")
                    for key, value in doc.items():
                        print(f"{key}: {value}")
                    print("--- End Document ---")
            else:
                print("⚠️ No loan_events documents found")
        else:
            print("❌ 'loan_events' collection not found")
            
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    debug_event_collections()
