#!/usr/bin/env python3
"""
Script to check actual reading data for member MEM03052026001
"""

import pymongo
from datetime import datetime
from collections import Counter

# Connect to MongoDB
client = pymongo.MongoClient("mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
db = client["goandstudydb"]

# Try to find the specific member MEM03052026001
member_id = "MEM03052026001"
member = db.members.find_one({"memberId": member_id})

if not member:
    print(f"\nMember {member_id} not found! Checking alternative queries...")
    # Try different query patterns
    member = db.members.find_one({"id": member_id})
    if not member:
        member = db.members.find_one({"_id": member_id})
    
    if not member:
        print(f"Member {member_id} not found with any query pattern!")
        exit(1)
    else:
        print(f"Found member with alternative query: {member.get('name', 'Unknown')}")
else:
    print(f"\nMember found: {member.get('name', 'Unknown')}")

# Add sample returned loans for this member if none exist
existing_loans = list(db.loans.find({"memberId": member_id, "status": "Returned"}))
if len(existing_loans) == 0:
    print(f"No returned loans found for {member_id}. Adding sample data...")
    
    # Get some books
    books = list(db.books.find().limit(5))
    if len(books) < 3:
        print("Not enough books in database")
        exit(1)
    
    # Create sample returned loans for this member
    from datetime import datetime, timedelta
    import random
    
    returned_loans = []
    for i, book in enumerate(books[:3]):
        issued_date = datetime.now() - timedelta(days=random.randint(30, 90))
        returned_date = issued_date + timedelta(days=random.randint(7, 21))
        
        loan = {
            "loanId": f"LN{datetime.now().strftime('%Y%m%d%H%M%S')}{i:02d}",
            "memberId": member_id,
            "bookId": book.get("id", book.get("_id")),
            "bookTitle": book.get("title", "Unknown Book"),
            "bookIsbn": book.get("isbn", ""),
            "issuedAt": issued_date,
            "dueDate": issued_date + timedelta(days=14),
            "returnedAt": returned_date,
            "status": "Returned",
            "renewalCount": random.randint(0, 2),
            "isOverdue": False,
            "overdueDays": 0,
            "fineAmount": 0,
            "finePaid": 0,
            "isCurrentlyReading": False,
            "createdAt": issued_date,
            "updatedAt": returned_date,
            "createdBy": "system",
            "updatedBy": "system"
        }
        returned_loans.append(loan)
    
    # Insert the loans
    result = db.loans.insert_many(returned_loans)
    print(f"Inserted {len(result.inserted_ids)} returned loans for {member_id}")
else:
    print(f"Found {len(existing_loans)} existing returned loans for {member_id}")

# Check returned loans for this member
returned_loans = list(db.loans.find({
    "memberId": member_id,
    "status": "Returned"
}))

print(f"\nReturned loans: {len(returned_loans)}")

# Analyze genres from returned loans
genre_counts = Counter()
book_categories = {}

for loan in returned_loans:
    book_id = loan.get("bookId")
    if book_id:
        book = db.books.find_one({"id": book_id})
        if book:
            print(f"  Book: {book.get('title', 'Unknown')} - Full book data keys: {list(book.keys())}")
            
            # Try different category field names
            categories = book.get("categoryIds", [])
            if not categories:
                categories = book.get("categoryIds", [])
            if not categories:
                categories = book.get("categories", [])
            if not categories and "category" in book:
                categories = [book["category"]]
            
            print(f"    Categories found: {categories}")
            
            # If no categories, assign a default genre based on book title or use "General"
            if not categories:
                genre_name = "General"
                genre_counts[genre_name] += 1
                book_categories[genre_name] = {"name": genre_name}
            else:
                for cat_id in categories:
                    category = db.book_categories.find_one({"id": cat_id})
                    if category:
                        genre_name = category.get("name", "Unknown")
                        genre_counts[genre_name] += 1
                        book_categories[genre_name] = category
                    else:
                        # If category not found, use the ID as genre name
                        genre_name = f"Category {cat_id}"
                        genre_counts[genre_name] += 1
                        book_categories[genre_name] = {"name": genre_name}

print(f"\nGenre breakdown:")
total_books = len(returned_loans)
for genre, count in genre_counts.most_common():
    percentage = (count / total_books * 100) if total_books > 0 else 0
    print(f"  {genre}: {count} books ({percentage:.1f}%)")

# Trigger analytics recomputation for this member
print(f"\nTriggering analytics recomputation for {member_id}...")
try:
    # This would typically be called from backend, but we'll prepare the data structure
    genre_breakdown = []
    for genre, count in genre_counts.most_common():
        percentage = (count / total_books * 100) if total_books > 0 else 0
        genre_breakdown.append({
            "genre": genre,
            "count": count,
            "percentage": percentage
        })
    
    print(f"Genre breakdown data ready: {genre_breakdown}")
    
    # Update or create analytics document
    analytics_data = {
        "id": member_id,
        "totalReturned": len(returned_loans),
        "genreBreakdown": genre_breakdown,
        "computedAt": datetime.now()
    }
    
    # Use upsert to update or create
    db.member_analytics.update_one(
        {"id": member_id},
        {"$set": analytics_data},
        upsert=True
    )
    
    print(f"Analytics updated for {member_id}")
    
except Exception as e:
    print(f"Error updating analytics: {e}")

# Check existing analytics
analytics = db.member_analytics.find_one({"id": member_id})
if analytics:
    print(f"\nFinal analytics:")
    print(f"  Total returned: {analytics.get('totalReturned', 0)}")
    print(f"  Genre breakdown: {analytics.get('genreBreakdown', [])}")

client.close()
