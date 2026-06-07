#!/usr/bin/env python3
"""
Check Twelfth Night book data in MongoDB directly
"""
import pymongo
import json

# Connection string - same as backend
MONGODB_URI = "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster"
DB_NAME = "goandstudydb"

def check_twelfth_night_in_mongo():
    print("=" * 70)
    print("CHECKING TWELFTH NIGHT IN MONGODB")
    print("=" * 70)
    
    try:
        client = pymongo.MongoClient(MONGODB_URI, serverSelectionTimeoutMS=5000)
        db = client[DB_NAME]
        
        # Test connection
        db.command('ping')
        print("✓ Connected to MongoDB\n")
        
        # Find Twelfth Night
        books_collection = db['books']
        print("[1] Searching for Twelfth Night book...")
        
        twelfth_night = books_collection.find_one(
            {"title": {"$regex": "Twelfth Night", "$options": "i"}}
        )
        
        if not twelfth_night:
            print("✗ Twelfth Night not found")
            return
        
        print("✓ Found Twelfth Night\n")
        print("Book Data:")
        print(f"  _id: {twelfth_night.get('_id')}")
        print(f"  title: {twelfth_night.get('title')}")
        print(f"  isbn: {twelfth_night.get('isbn')}")
        print(f"  isDeleted: {twelfth_night.get('isDeleted', False)}")
        print(f"  authorIds: {twelfth_night.get('authorIds', [])}")
        print(f"  categoryIds: {twelfth_night.get('categoryIds', [])}")
        
        # Check if authors exist
        author_ids = twelfth_night.get('authorIds', [])
        if author_ids:
            print(f"\n[2] Checking {len(author_ids)} authors...")
            authors_collection = db['authors']
            for author_id in author_ids:
                author = authors_collection.find_one({"_id": author_id})
                if author:
                    print(f"  ✓ Author {author_id}: {author.get('name', 'Unknown')}")
                else:
                    print(f"  ✗ Author {author_id} NOT FOUND in database!")
        
        # Check if categories exist
        category_ids = twelfth_night.get('categoryIds', [])
        if category_ids:
            print(f"\n[3] Checking {len(category_ids)} categories...")
            categories_collection = db['book_categories']
            for category_id in category_ids:
                category = categories_collection.find_one({"_id": category_id})
                if category:
                    print(f"  ✓ Category {category_id}: {category.get('name', 'Unknown')}")
                else:
                    print(f"  ✗ Category {category_id} NOT FOUND in database!")
        
        # Check if there are any reviews
        print(f"\n[4] Checking reviews...")
        reviews_collection = db['book_reviews']
        reviews_count = reviews_collection.count_documents(
            {"bookId": str(twelfth_night.get('_id'))}
        )
        print(f"  Reviews for this book: {reviews_count}")
        
        print("\n✓ Data integrity check complete")
        
        client.close()
        
    except pymongo.errors.ServerSelectionTimeoutError:
        print("✗ Cannot connect to MongoDB - server is not responding")
    except Exception as e:
        print(f"✗ Error: {e}")

if __name__ == "__main__":
    check_twelfth_night_in_mongo()
