import os
from pymongo import MongoClient
from dotenv import load_dotenv
from datetime import datetime, timedelta
import random
load_dotenv()

client = MongoClient(os.getenv('MONGODB_URI'))
db = client.library_db

# Check if we have the book and member
book = db.books.find_one({'isbn': '978-0-452-28423-4'})  # 1984 book
member = db.members.find_one({'email': 'sayantanj03@gmail.com'})

if book and member:
    # Create an active loan
    loan_id = f'LN{random.randint(100000, 999999)}'
    due_date = datetime.now() + timedelta(days=14)
    
    loan = {
        'id': loan_id,
        '_id': loan_id,
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
    db.members.update_one({'id': member['id']}, {'$inc': {'activeLoanCount': 1}})
    
    # Update book available copies
    db.books.update_one({'_id': book['_id']}, {'$inc': {'availableCopies': -1}})
    
    print(f'Created loan: {loan_id}')
    print(f"Member: {member['email']} ({member['id']})")
    print(f"Book: {book['title']} ({book['isbn']})")
    print(f'Due: {due_date}')
else:
    print(f'Book found: {book is not None}')
    print(f'Member found: {member is not None}')
    
client.close()
