import pymongo
from datetime import datetime

# Connect to MongoDB
client = pymongo.MongoClient('mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/goandstudydb?retryWrites=true&w=majority')
db = client.goandstudydb

print('=== Creating Overdue Loans for Testing ===')
# Find active loans that should be overdue
active_loans = list(db.loans.find({'status': {'$ne': 'RETURNED'}}))
print(f'Found {len(active_loans)} active loans')

for loan in active_loans:
    due_date = loan.get('dueDate')
    if due_date:
        # Check if loan is actually overdue
        if isinstance(due_date, str):
            due_dt = datetime.fromisoformat(due_date.replace('Z', '+00:00'))
        else:
            due_dt = due_date
        
        if due_dt < datetime.now():
            print(f'Loan {loan.get("_id")} should be overdue!')
            print(f'  Due: {due_date}')
            print(f'  Current status: {loan.get("status")}')
            print(f'  Current overdue days: {loan.get("overdueDays")}')
            print(f'  Current fine amount: {loan.get("fineAmount")}')
            print()

# Check if there are any overdue loans in the database
overdue_loans = list(db.loans.find({'status': 'Overdue'}))
print(f'Found {len(overdue_loans)} loans with status "Overdue"')

client.close()
