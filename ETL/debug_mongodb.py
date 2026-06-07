#!/usr/bin/env python3
"""
Debug MongoDB collections and data
"""
import os
import sys
from pymongo import MongoClient
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def debug_mongodb():
    """Debug MongoDB connection and collections"""
    print("🔍 Debugging MongoDB Atlas Connection")
    print("=" * 50)
    
    # Get connection details
    uri = os.getenv('MONGO_URI')
    db_name = os.getenv('MONGO_DB_NAME', 'GoAndStudy')
    
    print(f"📡 MongoDB URI: {uri}")
    print(f"📊 Database Name: {db_name}")
    print()
    
    try:
        # Connect to MongoDB
        client = MongoClient(uri)
        print("✅ Connected to MongoDB Atlas")
        
        # Test connection
        client.admin.command('ping')
        print("✅ Connection test successful")
        print()
        
        # Get database
        db = client[db_name]
        print(f"📋 Database: {db.name}")
        print()
        
        # List all databases first
        print("🗄️ Available Databases:")
        all_databases = client.list_database_names()
        for i, db_name in enumerate(all_databases, 1):
            print(f"   {i:2d}. {db_name}")
        print()
        
        # List all collections
        print("📁 Available Collections:")
        collections = db.list_collection_names()
        
        if not collections:
            print("   ❌ No collections found!")
            print(f"   💡 Trying other databases...")
            
            # Check other databases for collections
            for other_db_name in all_databases:
                if other_db_name != db_name:
                    other_db = client[other_db_name]
                    other_collections = other_db.list_collection_names()
                    if other_collections:
                        print(f"   🎯 Found {len(other_collections)} collections in '{other_db_name}' database:")
                        for coll in other_collections[:5]:  # Show first 5
                            count = other_db[coll].count_documents({})
                            print(f"      • {coll}: {count:,} documents")
                        break
        else:
            for i, collection_name in enumerate(collections, 1):
                collection = db[collection_name]
                count = collection.count_documents({})
                print(f"   {i:2d}. {collection_name:30} - {count:,} documents")
        
        print()
        print(f"📈 Total Collections: {len(collections)}")
        
        # Show sample data from first few collections
        if collections:
            print("\n🔍 Sample Data Analysis:")
            for collection_name in collections[:3]:  # First 3 collections
                collection = db[collection_name]
                sample = collection.find_one()
                if sample:
                    print(f"\n📄 {collection_name} - Sample Document:")
                    for key, value in list(sample.items())[:5]:  # First 5 fields
                        print(f"   • {key}: {type(value).__name__} = {str(value)[:50]}...")
                else:
                    print(f"\n📄 {collection_name} - Empty collection")
        
        client.close()
        print("\n✅ Debug completed successfully")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
    
    return True

if __name__ == "__main__":
    debug_mongodb()
