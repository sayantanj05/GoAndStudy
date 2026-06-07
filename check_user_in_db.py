from pymongo import MongoClient
import json

MONGO_URI = "mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster"
DATABASE = "goandstudydb"

try:
    print("=" * 60)
    print("CHECKING MONGODB CONNECTION AND USER DATA")
    print("=" * 60)
    
    # Connect to MongoDB
    print("\n[1] Connecting to MongoDB...")
    client = MongoClient(MONGO_URI)
    
    # Try to access admin database to verify connection
    try:
        client.admin.command('ping')
        print("    ✓ MongoDB connection successful!")
    except Exception as e:
        print(f"    ✗ MongoDB ping failed: {e}")
        exit(1)
    
    db = client[DATABASE]
    
    # Check if users collection exists
    print("\n[2] Checking users collection...")
    collections = db.list_collection_names()
    print(f"    Available collections: {collections[:10]}")
    
    if 'users' in collections:
        users_count = db.users.count_documents({})
        print(f"    ✓ Users collection found with {users_count} documents")
        
        # Check for our test user
        print("\n[3] Searching for test user...")
        test_user = db.users.find_one({"email": "sayantanj08@gmail.com"})
        
        if test_user:
            print("    ✓ User found!")
            print(f"    Email: {test_user.get('email')}")
            print(f"    Role: {test_user.get('role')}")
            print(f"    Password hash exists: {'password' in test_user}")
            if 'password' in test_user:
                pwd = test_user.get('password')
                print(f"    Password: {pwd[:50] if len(pwd) > 50 else pwd}...")
        else:
            print("    ✗ User NOT found in database!")
            
            # Show some users for reference
            print("\n[4] Sample users in database:")
            sample_users = list(db.users.find({}).limit(3))
            for i, user in enumerate(sample_users, 1):
                print(f"    User {i}: {user.get('email')} (Role: {user.get('role')})")
    else:
        print("    ✗ Users collection not found!")
        
except Exception as e:
    print(f"\n✗ Error: {type(e).__name__}: {str(e)}")
    import traceback
    traceback.print_exc()
