#!/usr/bin/env python3
"""
Check the actual role of analytics@gmail.com user in the database
"""

import pymongo
from pymongo import MongoClient
import os

def check_user_role():
    """Check the role of analytics@gmail.com user in MongoDB"""
    
    try:
        # Connect to MongoDB (adjust connection string as needed)
        client = MongoClient("mongodb://localhost:27017")
        db = client["goandstudy"]
        
        print("🔍 Checking user role in database...")
        
        # Check in staff collection
        staff_user = db["staff"].find_one({"email": "analytics@gmail.com"})
        if staff_user:
            print(f"✅ Found user in staff collection:")
            print(f"   - Email: {staff_user.get('email')}")
            print(f"   - Name: {staff_user.get('name')}")
            print(f"   - Role: {staff_user.get('role')}")
            print(f"   - ID: {staff_user.get('id')}")
            print(f"   - Active: {staff_user.get('isActive')}")
            return staff_user
        
        # Check in members collection
        member_user = db["members"].find_one({"email": "analytics@gmail.com"})
        if member_user:
            print(f"✅ Found user in members collection:")
            print(f"   - Email: {member_user.get('email')}")
            print(f"   - Name: {member_user.get('name')}")
            print(f"   - Role: {member_user.get('role')}")
            print(f"   - ID: {member_user.get('id')}")
            return member_user
        
        print("❌ User not found in database")
        return None
        
    except Exception as e:
        print(f"❌ Error connecting to database: {e}")
        return None

def update_user_role():
    """Update the user role from ROLE_STAFF to ROLE_ANALYTICS_ENGINEER"""
    
    try:
        client = MongoClient("mongodb://localhost:27017")
        db = client["goandstudy"]
        
        print("\n🔧 Updating user role to ROLE_ANALYTICS_ENGINEER...")
        
        result = db["staff"].update_one(
            {"email": "analytics@gmail.com"},
            {"$set": {"role": "ROLE_ANALYTICS_ENGINEER"}}
        )
        
        if result.modified_count > 0:
            print("✅ User role updated successfully!")
            return True
        else:
            print("⚠️  No changes made - user might already have correct role")
            return False
            
    except Exception as e:
        print(f"❌ Error updating user role: {e}")
        return False

def main():
    print("🚀 Checking and Fixing User Role")
    print("=" * 40)
    
    # Check current role
    user = check_user_role()
    
    if user:
        current_role = user.get('role')
        print(f"\n📋 Current role: {current_role}")
        
        if current_role == "ROLE_STAFF":
            print("⚠️  User has ROLE_STAFF - this is why they can access staff portal!")
            
            # Ask for confirmation
            response = input("\n❓ Update role to ROLE_ANALYTICS_ENGINEER? (y/n): ")
            if response.lower() == 'y':
                if update_user_role():
                    print("\n🎉 Fix applied! User should now be redirected to analytics dashboard.")
                    print("   Restart the backend to ensure changes take effect.")
                else:
                    print("\n❌ Failed to update role")
            else:
                print("\n❌ Role update cancelled")
        elif current_role == "ROLE_ANALYTICS_ENGINEER":
            print("✅ User already has correct role")
        else:
            print(f"❓ Unexpected role: {current_role}")
    else:
        print("❌ Could not find user in database")

if __name__ == "__main__":
    main()
