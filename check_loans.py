import pymongo
from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

# Connect to MongoDB
client = MongoClient(os.getenv('MONGODB_URI'))
db = client.goandstudy

# Check active loans
loans = list(db.loans.find({'status': {'$in': ['ISSUED', 'RENEWED', 'OVERDUE']}}))
print(f'Found {len(loans)} active loans')
for loan in loans:
    print(f'ISBN: {loan.get("bookIsbn", "N/A")}, Title: {loan.get("bookTitle", "N/A")}, Status: {loan.get("status", "N/A")}, Member: {loan.get("memberId", "N/A")}')

print('\n---\n')

# Check specifically for ISBN 9781643499000 (normalized)
target_isbn = '9781643499000'
matching_loans = list(db.loans.find({
    'status': {'$in': ['ISSUED', 'RENEWED', 'OVERDUE']},
    '$or': [
        {'bookIsbn': target_isbn},
        {'bookIsbn': target_isbn.replace('-', '')},
        {'bookIsbn': f'{target_isbn[:3]}-{target_isbn[3:]}'}
    ]
}))
print(f'Found {len(matching_loans)} loans for ISBN {target_isbn}')
for loan in matching_loans:
    print(f'Loan ID: {loan.get("_id")}, ISBN: {loan.get("bookIsbn")}, Title: {loan.get("bookTitle")}')
