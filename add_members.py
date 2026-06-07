#!/usr/bin/env python3
"""
Script to add members to the database
"""

import pymongo
from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

# Connect to MongoDB
client = MongoClient(os.getenv('MONGODB_URI'))
db = client.goandstudy

# Sample members data
sample_members = [
    {
        "_id": "MEM001",
        "email": "sayantanj03@gmail.com",
        "name": "Sayantan Jana",
        "passwordHash": "$2a$12$WGk7/lhDBQRTxB2Hyz8AMuEE9cM0v1bBtMaWRUJ74oOMeou9otCQS",
        "role": "ROLE_MEMBER",
        "isActive": True,
        "membershipDate": "2026-01-01T00:00:00Z",
        "loanDurationDays": 14,
        "fineRatePerDay": 10.0,
        "maxLoansAllowed": 5,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    },
    {
        "_id": "MEM002",
        "email": "arjit@gmail.com",
        "name": "Arijit Kumar",
        "passwordHash": "$2a$12$somehash",
        "role": "ROLE_MEMBER",
        "isActive": True,
        "membershipDate": "2026-01-01T00:00:00Z",
        "loanDurationDays": 14,
        "fineRatePerDay": 10.0,
        "maxLoansAllowed": 5,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    },
    {
        "_id": "MEM003",
        "email": "rahul@gmail.com",
        "name": "Rahul Sharma",
        "passwordHash": "$2a$12$somehash",
        "role": "ROLE_MEMBER",
        "isActive": True,
        "membershipDate": "2026-01-01T00:00:00Z",
        "loanDurationDays": 14,
        "fineRatePerDay": 10.0,
        "maxLoansAllowed": 5,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    }
]

# Sample staff data
sample_staff = [
    {
        "_id": "STAFF001",
        "email": "staff0001@gmail.com",
        "name": "Staff Member 1",
        "passwordHash": "$2a$12$BcXSBcCWnHpZa3mJkTHp5Oz2l9v5DYc8x8vpMuqI99Z.tZAdrwCRy",
        "role": "ROLE_STAFF",
        "isActive": True,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    },
    {
        "_id": "STAFF002",
        "email": "analytics@gmail.com",
        "name": "Analytics Staff",
        "passwordHash": "$2a$12$Y5rZrJmzmT9T3Yer5/S9KOQ8q1DU/BuXxs6dAEB",
        "role": "ROLE_STAFF",
        "isActive": True,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    }
]

# Sample admin data
sample_admins = [
    {
        "_id": "ADMIN001",
        "email": "admin@gmail.com",
        "name": "Admin User",
        "passwordHash": "$2a$12$WGk7/lhDBQRTxB2Hyz8AMuEE9cM0v1bBtMaWRUJ74oOMeou9otCQS",
        "role": "ROLE_ADMIN",
        "isActive": True,
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
    }
]

def main():
    try:
        print("Adding members, staff, and admins to database...")
        
        # Clear existing data
        db.members.delete_many({})
        db.staff.delete_many({})
        db.admins.delete_many({})
        
        # Insert members
        result = db.members.insert_many(sample_members)
        print(f"Inserted {len(result.inserted_ids)} members")
        
        # Insert staff
        result = db.staff.insert_many(sample_staff)
        print(f"Inserted {len(result.inserted_ids)} staff")
        
        # Insert admins
        result = db.admins.insert_many(sample_admins)
        print(f"Inserted {len(result.inserted_ids)} admins")
        
        # Verify insertion
        members_count = db.members.count_documents({})
        staff_count = db.staff.count_documents({})
        admins_count = db.admins.count_documents({})
        
        print(f"\nDatabase now contains:")
        print(f"- {members_count} members")
        print(f"- {staff_count} staff") 
        print(f"- {admins_count} admins")
        
        print("\nMembers added:")
        for member in db.members.find({}):
            print(f"- {member['name']} ({member['email']})")
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    main()
