from pymongo import MongoClient
from bson import ObjectId

client = MongoClient(
    'mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster',
    tlsAllowInvalidCertificates=True
)
db = client['goandstudydb']

# Check loans collection structure
loan = db.loans.find_one()
print("Sample loan document:")
for k, v in sorted(loan.items()):
    print(f"  {k}: {v} (type: {type(v).__name__})")

print(f"\nTotal loans: {db.loans.count_documents({})}")

# Check if _id is ObjectId
print(f"\n_id type: {type(loan['_id']).__name__}")
print(f"_id value: {loan['_id']}")

# Check issuedAt field
print(f"\nissuedAt field present: {'issuedAt' in loan}")
if 'issuedAt' in loan:
    print(f"issuedAt value: {loan['issuedAt']}")

# Check if loan has custom id field
for field in ['loanId', 'id', 'loan_id']:
    if field in loan:
        print(f"{field}: {loan[field]}")
