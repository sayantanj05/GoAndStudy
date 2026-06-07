#!/usr/bin/env python3
"""
Check member details and loan history for MEM03052026001
"""

import pymongo
from datetime import datetime

# Connect to MongoDB
client = pymongo.MongoClient("mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster")
db = client["goandstudydb"]

member_id = "MEM03052026001"

# Find member details
member = db.members.find_one({"memberId": member_id})
if member:
    print(f"Member Details:")
    print(f"  ID: {member.get('memberId', 'Unknown')}")
    print(f"  Name: {member.get('name', 'Unknown')}")
    print(f"  Email: {member.get('email', 'Unknown')}")
    print(f"  Phone: {member.get('phone', 'Unknown')}")
    print(f"  Gender: {member.get('gender', 'Unknown')}")
    print(f"  Membership Type: {member.get('membershipType', 'Unknown')}")
    print(f"  Registration Date: {member.get('timeCreated', 'Unknown')}")
    print(f"  Total Loans: {member.get('totalLoans', 0)}")
    print(f"  Total Fines Paid: {member.get('totalFinesPaid', 0)}")
else:
    print(f"Member {member_id} not found!")

print(f"\n" + "="*50)

# Get all loans for this member
all_loans = list(db.loans.find({"memberId": member_id}))
print(f"Total Loans Found: {len(all_loans)}")

if all_loans:
    print(f"\nLoan History:")
    print(f"{'Loan ID':<15} {'Title':<30} {'Status':<12} {'Issued Date':<15} {'Due Date':<15} {'Returned Date':<15} {'Fine':<8}")
    print("-" * 100)
    
    for i, loan in enumerate(all_loans, 1):
        loan_id = loan.get("loanId", "Unknown")
        title = loan.get("bookTitle", "Unknown")
        status = loan.get("status", "Unknown")
        issued_date = loan.get("issuedAt", "Unknown")
        due_date = loan.get("dueDate", "Unknown")
        returned_date = loan.get("returnedAt", "Not returned")
        fine_amount = loan.get("fineAmount", 0)
        
        # Format dates for display
        if isinstance(issued_date, datetime):
            issued_str = issued_date.strftime("%Y-%m-%d %H:%M")
        else:
            issued_str = str(issued_date)
            
        if isinstance(due_date, datetime):
            due_str = due_date.strftime("%Y-%m-%d %H:%M")
        else:
            due_str = str(due_date)
            
        if isinstance(returned_date, datetime):
            returned_str = returned_date.strftime("%Y-%m-%d %H:%M")
        else:
            returned_str = str(returned_date)
        
        print(f"{i:<4} {loan_id:<20} {title[:28]:<30} {status:<12} {issued_str:<15} {due_str:<15} {returned_str:<15} ₹{fine_amount:<7}")

else:
    print("No loans found for this member")

# Get analytics
analytics = db.member_analytics.find_one({"id": member_id})
if analytics:
    print(f"\n" + "="*50)
    print(f"Analytics Summary:")
    print(f"  Books Read: {analytics.get('totalReturned', 0)}")
    print(f"  Total Loans: {analytics.get('totalLoans', 0)}")
    print(f"  Average Loan Duration: {analytics.get('avgLoanDuration', 0):.1f} days")
    print(f"  Total Overdue: {analytics.get('totalOverdue', 0)}")
    print(f"  Overdue Rate: {analytics.get('overdueRate', 0):.1%}")

client.close()
