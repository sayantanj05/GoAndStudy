#!/usr/bin/env python3
"""
Get all categories from database
"""

import pymongo

# Connect to MongoDB
client = pymongo.MongoClient("mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
db = client["goandstudydb"]

# Get all categories
categories = list(db.book_categories.find())
print(f"Total categories: {len(categories)}")

print("\nAll available categories:")
for cat in categories:
    print(f"  - {cat.get('_id', 'Unknown')}: {cat.get('name', 'Unknown')}")

client.close()
