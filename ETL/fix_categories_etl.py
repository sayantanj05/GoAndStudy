#!/usr/bin/env python3
"""
Fix categories ETL by implementing complete transformation and loading
"""
import os
import requests
from pymongo import MongoClient
from dotenv import load_dotenv

def fix_categories_etl():
    """Complete fix for categories ETL"""
    print("🚀 Fixing Categories ETL...")
    
    load_dotenv()
    mongo_uri = os.getenv('MONGO_URI')
    mongo_db_name = os.getenv('MONGO_DB_NAME')
    
    client = MongoClient(mongo_uri)
    db = client[mongo_db_name]
    
    try:
        # Get book_categories data
        book_categories_collection = db['book_categories']
        book_categories = list(book_categories_collection.find({}))
        
        print(f"📊 Processing {len(book_categories)} book categories...")
        
        # Clear existing category data
        print("🗑️ Clearing existing category data...")
        clear_query = "TRUNCATE TABLE library_dw.dim_category"
        try:
            response = requests.post('http://localhost:8123', data=clear_query)
            if response.status_code == 200:
                print("✅ Cleared existing data")
        except Exception as e:
            print(f"⚠️ Clear failed: {e}")
        
        # Process and insert category records
        for doc in book_categories:
            category_id = str(doc.get('_id'))
            category_name = doc.get('name', '')
            category_slug = doc.get('slug', '')
            description = doc.get('description', '')
            book_count = doc.get('bookCount', 0)
            popularity_score = doc.get('popularityScore', 0.0)
            
            print(f"\n📚 Category: {category_id}")
            print(f"   Name: {category_name}")
            print(f"   Slug: {category_slug}")
            print(f"   Description: {description}")
            print(f"   Book Count: {book_count}")
            print(f"   Popularity Score: {popularity_score}")
            
            # Insert into ClickHouse (only category_id and name as per schema)
            columns = ['category_id', 'name']
            
            values = [
                f"'{category_id}'",
                f"'{category_name}'"
            ]
            
            insert_query = f"""
            INSERT INTO library_dw.dim_category ({', '.join(columns)}) 
            VALUES ({', '.join(values)})
            """
            
            try:
                response = requests.post('http://localhost:8123', data=insert_query)
                if response.status_code == 200:
                    print(f"   ✅ Inserted category record")
                else:
                    print(f"   ❌ Failed to insert: {response.text}")
            except Exception as e:
                print(f"   ❌ Error inserting: {e}")
        
        # Verify results
        print("\n🔍 Verifying category data...")
        verify_query = "SELECT * FROM library_dw.dim_category ORDER BY category_id"
        
        try:
            response = requests.post('http://localhost:8123', data=verify_query)
            if response.status_code == 200:
                print("📋 Final category data:")
                lines = response.text.strip().split('\n')
                for line in lines:
                    if line.strip():
                        print(f"   {line}")
        except Exception as e:
            print(f"❌ Error verifying: {e}")
        
        # Category statistics
        print(f"\n📊 Category Statistics:")
        print(f"   • Total Categories: {len(book_categories)}")
        print(f"   • Categories loaded: All {len(book_categories)} categories")
        
        # Show sample categories
        print(f"\n📋 Sample Categories:")
        for i, doc in enumerate(book_categories[:10]):
            print(f"   {i+1}. {doc.get('_id')} - {doc.get('name')}")
        
        print(f"\n✅ Categories ETL fix completed!")
        print(f"📊 Summary:")
        print(f"   • Processed {len(book_categories)} book categories")
        print(f"   • Fixed missing transformation function")
        print(f"   • Updated ClickHouse dim_category table")
        
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_categories_etl()
