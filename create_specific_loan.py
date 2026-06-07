import os
from pymongo import MongoClient
from dotenv import load_dotenv
from datetime import datetime, timedelta
import random
load_dotenv()

client = MongoClient(os.getenv('MONGODB_URI'))
db = client.library_db

# Add the specific book with ISBN 9781398543454 (if not exists)
book = db.books.find_one({'isbn': '9781398543454'})
if not book:
    book = {
        '_id': f'BK{random.randint(100000, 999999)}',
        'id': f'BK{random.randint(100000, 999999)}',
        'isbn': '9781398543454',
        'title': 'Sample Book for Testing',
        'authorIds': [],
        'categoryIds': [],
        'description': 'Test book for return process',
        'language': 'en',
        'publishedYear': 2024,
        'pageCount': 300,
        'totalCopies': 5,
        'availableCopies': 4,
        'reservedCopies': 0,
        'averageRating': 0.0,
        'totalRatings': 0,
        'totalIssues': 1,
        'isDeleted': False,
        'createdAt': datetime.now(),
        'updatedAt': datetime.now()
    }
    db.books.insert_one(book)
    print(f"Created book: {book['title']} (ISBN: {book['isbn']})")
else:
    print(f"Book exists: {book['title']} (ISBN: {book['isbn']})")

# Check for member - use the available email closest to sayan@gmail.com
member = db.members.find_one({'email': 'sayantanj03@gmail.com'})
if not member:
    # Create member sayan@gmail.com
    member = {
        '_id': 'MB000001',
        'id': 'MB000001',
        'email': 'sayan@gmail.com',
        'name': 'Sayan User',
        'phone': '1234567890',
        'membershipType': 'Regular',
        'isActive': True,
        'activeLoanCount': 0,
        'totalLoans': 0,
        'createdAt': datetime.now(),
        'updatedAt': datetime.now()
    }
    db.members.insert_one(member)
    print(f"Created member: {member['email']} ({member['id']})")
else:
    print(f"Member exists: {member['email']} ({member['id']})")

# Check if active loan already exists
existing_loan = db.loans.find_one({
    'memberId': member['id'],
    'bookId': str(book['_id']),
    'status': {'$in': ['ISSUED', 'RENEWED', 'OVERDUE']}
})

if existing_loan:
    print(f"\nActive loan already exists: {existing_loan['id']}")
    print(f"  Member: {member['email']}")
    print(f"  Book: {book['title']} ({book['isbn']})")
    print(f"  Status: {existing_loan['status']}")
    print(f"  Due: {existing_loan.get('dueDate')}")
else:
    # Create an active loan
    loan_id = f'LN{random.randint(100001, 999999)}'
    due_date = datetime.now() + timedelta(days=14)
    
    loan = {
        '_id': loan_id,
        'id': loan_id,
        'memberId': member['id'],
        'bookId': str(book['_id']),
        'bookIsbn': book['isbn'],
        'bookTitle': book['title'],
        'status': 'ISSUED',
        'issuedAt': datetime.now(),
        'dueDate': due_date,
        'renewalCount': 0,
        'overdueDays': 0,
        'fineAmount': 0.0,
        'finePaid': False,
        'isCurrentlyReading': False,
        'isOverdue': False,
        'createdAt': datetime.now(),
        'updatedAt': datetime.now()
    }
    
    db.loans.insert_one(loan)
    
    # Update member active loan count
    db.members.update_one({'id': member['id']}, {'$inc': {'activeLoanCount': 1, 'totalLoans': 1}})
    
    # Update book available copies
    db.books.update_one({'_id': book['_id']}, {'$inc': {'availableCopies': -1, 'totalIssues': 1}})
    
    print(f"\nCreated active loan: {loan_id}")
    print(f"  Member: {member['email']} ({member['id']})")
    print(f"  Book: {book['title']} ({book['isbn']})")
    print(f"  Due: {due_date}")

print("\n=== SUMMARY ===")
print(f"Book ISBN: {book['isbn']}")
print(f"Member: {member['email']}")
print(f"Can now test return process with ISBN: 9781398543454")

client.close()
