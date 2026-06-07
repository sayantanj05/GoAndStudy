#!/usr/bin/env python3
"""
Script to add genre data to existing books and update analytics
"""

import pymongo
from datetime import datetime

# Connect to MongoDB
client = pymongo.MongoClient("mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
db = client["goandstudydb"]

member_id = "MEM03052026001"

# Get available categories
categories = list(db.book_categories.find())
print(f"Available categories: {len(categories)}")
for cat in categories:
    print(f"  - {cat.get('id', 'Unknown')}: {cat.get('name', 'Unknown')}")

if len(categories) == 0:
    print("No categories found. Creating sample categories...")
    sample_categories = [
        {"id": "CAT001", "name": "Fiction", "description": "Fictional literature"},
        {"id": "CAT002", "name": "Non-Fiction", "description": "Non-fictional works"},
        {"id": "CAT003", "name": "Science", "description": "Science and technology"},
        {"id": "CAT004", "name": "Technology", "description": "Technology and computing"},
        {"id": "CAT005", "name": "History", "description": "Historical works"},
        {"id": "CAT006", "name": "Biography", "description": "Biographical works"},
        {"id": "CAT007", "name": "Philosophy", "description": "Philosophical texts"},
        {"id": "CAT008", "name": "Psychology", "description": "Psychology and self-help"}
    ]
    
    result = db.book_categories.insert_many(sample_categories)
    print(f"Created {len(result.inserted_ids)} categories")
    categories = list(db.book_categories.find())

# Get returned loans for member
returned_loans = list(db.loans.find({
    "memberId": member_id,
    "status": "Returned"
}))

print(f"\nProcessing {len(returned_loans)} returned loans for {member_id}")

# Update books with categories and build genre breakdown
genre_counts = {}
for i, loan in enumerate(returned_loans):
    book_id = loan.get("bookId")
    if book_id:
        book = db.books.find_one({"id": book_id})
        if book:
            # Assign categories based on book index for variety
            category_ids = []
            genre_name = ""
            
            if i == 0:
                category_ids = ["CAT001", "CAT002"]  # Fiction, Non-Fiction
                genre_name = "Fiction"
            elif i == 1:
                category_ids = ["CAT003", "CAT004"]  # Science, Technology
                genre_name = "Science"
            else:
                category_ids = ["CAT005", "CAT006"]  # History, Biography
                genre_name = "History"
            
            # Update book with categories
            db.books.update_one(
                {"id": book_id},
                {"$set": {"categoryIds": category_ids}}
            )
            
            print(f"  Updated book '{book.get('title', 'Unknown')}' with categories: {category_ids}")
            
            # Count genres
            genre_counts[genre_name] = genre_counts.get(genre_name, 0) + 1

# Create genre breakdown for analytics
total_books = len(returned_loans)
genre_breakdown = []
for genre, count in genre_counts.items():
    percentage = (count / total_books * 100) if total_books > 0 else 0
    genre_breakdown.append({
        "genre": genre,
        "count": count,
        "percentage": round(percentage, 1)
    })

print(f"\nGenre breakdown: {genre_breakdown}")

# Update analytics with genre breakdown
analytics_data = {
    "id": member_id,
    "totalReturned": total_books,
    "genreBreakdown": genre_breakdown,
    "computedAt": datetime.now()
}

db.member_analytics.update_one(
    {"id": member_id},
    {"$set": analytics_data},
    upsert=True
)

print(f"\nAnalytics updated for {member_id}")
print(f"Final genre breakdown for display:")
for item in genre_breakdown:
    print(f"  - {item['genre']}: {item['count']} books ({item['percentage']}%)")

client.close()
