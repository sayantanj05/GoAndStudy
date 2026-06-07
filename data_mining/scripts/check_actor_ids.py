from pymongo import MongoClient

client = MongoClient(
    'mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster',
    tlsAllowInvalidCertificates=True
)
db = client['goandstudydb']

# Check if MEM* IDs exist in members collection
for mid in ['MEM22032026001', 'MEM05042026002']:
    member = db.members.find_one({'_id': mid})
    print(f'members._id={mid}: {"EXISTS" if member else "NOT FOUND"}')

# Check staff and admin collections
staff = db.staff.find_one({'_id': 'ST001'})
admin = db.admin.find_one({'_id': 'AD001'})
print(f'staff._id=ST001: {"EXISTS" if staff else "NOT FOUND"}')
print(f'admin._id=AD001: {"EXISTS" if admin else "NOT FOUND"}')

# Count members by ID type
objid_count = db.members.count_documents({'_id': {'$type': 'objectId'}})
str_count = db.members.count_documents({'_id': {'$type': 'string'}})
print(f'Members with ObjectId: {objid_count}')
print(f'Members with String ID: {str_count}')

# Show sample member IDs
print('\nSample member _id values:')
for m in db.members.find().limit(5):
    print(f'  {m["_id"]} (type: {type(m["_id"]).__name__})')
