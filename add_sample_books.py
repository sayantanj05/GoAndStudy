#!/usr/bin/env python3
"""
Script to add sample books to the database for testing
"""

import pymongo
from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

# Connect to MongoDB
client = MongoClient(os.getenv('MONGODB_URI'))
db = client.goandstudy

# Sample books data
sample_books = [
    {
        "_id": "BOOK001",
        "title": "The Great Gatsby",
        "isbn": "978-0-7432-7356-5",
        "authorIds": ["AUTH001"],
        "categoryIds": ["CAT001"],
        "description": "A classic American novel",
        "availableCopies": 3,
        "totalCopies": 5,
        "coverImageUrl": "",
        "isDeleted": False,
        "totalIssues": 0,
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    },
    {
        "_id": "BOOK002", 
        "title": "To Kill a Mockingbird",
        "isbn": "978-0-06-112008-4",
        "authorIds": ["AUTH002"],
        "categoryIds": ["CAT001"],
        "description": "A gripping tale of racial injustice",
        "availableCopies": 2,
        "totalCopies": 4,
        "coverImageUrl": "",
        "isDeleted": False,
        "totalIssues": 0,
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    },
    {
        "_id": "BOOK003",
        "title": "1984",
        "isbn": "978-0-452-28423-4",
        "authorIds": ["AUTH003"],
        "categoryIds": ["CAT002"],
        "description": "A dystopian social science fiction novel",
        "availableCopies": 4,
        "totalCopies": 6,
        "coverImageUrl": "",
        "isDeleted": False,
        "totalIssues": 0,
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    }
]

# Sample authors
sample_authors = [
    {
        "_id": "AUTH001",
        "name": "F. Scott Fitzgerald",
        "bio": "American novelist and short story writer",
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    },
    {
        "_id": "AUTH002", 
        "name": "Harper Lee",
        "bio": "American novelist best known for To Kill a Mockingbird",
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    },
    {
        "_id": "AUTH003",
        "name": "George Orwell",
        "bio": "English novelist and essayist",
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    }
]

# Sample categories
sample_categories = [
    {
        "_id": "CAT001",
        "name": "Classic Literature",
        "description": "Classic literary works",
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    },
    {
        "_id": "CAT002",
        "name": "Science Fiction", 
        "description": "Science fiction novels",
        "createdAt": "2026-05-09T00:00:00Z",
        "updatedAt": "2026-05-09T00:00:00Z"
    }
]

def main():
    try:
        print("Adding sample data to database...")
        
        # Clear existing sample data
        db.books.delete_many({})
        db.authors.delete_many({})
        db.book_categories.delete_many({})
        
        # Insert categories
        result = db.book_categories.insert_many(sample_categories)
        print(f"Inserted {len(result.inserted_ids)} categories")
        
        # Insert authors
        result = db.authors.insert_many(sample_authors)
        print(f"Inserted {len(result.inserted_ids)} authors")
        
        # Insert books
        result = db.books.insert_many(sample_books)
        print(f"Inserted {len(result.inserted_ids)} books")
        
        # Verify insertion
        books_count = db.books.count_documents({})
        authors_count = db.authors.count_documents({})
        categories_count = db.book_categories.count_documents({})
        
        print(f"\nDatabase now contains:")
        print(f"- {books_count} books")
        print(f"- {authors_count} authors") 
        print(f"- {categories_count} categories")
        
        print("\nSample books added:")
        for book in db.books.find({}):
            print(f"- {book['title']} (ISBN: {book['isbn']})")
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    main()
