import pymongo
from datetime import datetime

# Connect to MongoDB
client = pymongo.MongoClient('mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster')
db = client.GoAndStudy

# Check loan collection structure
print('=== Loan Collection Structure ===')
loan_sample = db.loans.find_one()
if loan_sample:
    print('Sample loan document:')
    for key, value in loan_sample.items():
        print(f'  {key}: {type(value)} = {value}')
else:
    print('No loans found')

print('\n=== Check for fineAmount/overdueDays fields ===')
loans_with_fines = db.loans.count_documents({'fineAmount': {'$exists': True}})
loans_with_overdue = db.loans.count_documents({'overdueDays': {'$exists': True}})
print(f'Loans with fineAmount field: {loans_with_fines}')
print(f'Loans with overdueDays field: {loans_with_overdue}')

print('\n=== Check overdue loans ===')
overdue_loans = list(db.loans.find({'status': 'Overdue'}).limit(3))
for loan in overdue_loans:
    print(f'Loan ID: {loan.get("id")}')
    print(f'  Due Date: {loan.get("dueDate")}')
    print(f'  Status: {loan.get("status")}')
    print(f'  Fine Amount: {loan.get("fineAmount", "NOT_FOUND")}')
    print(f'  Overdue Days: {loan.get("overdueDays", "NOT_FOUND")}')
    print()

client.close()
