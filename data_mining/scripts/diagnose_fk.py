from pymongo import MongoClient

client = MongoClient(
    'mongodb+srv://goandstudyadmin:goandstudyadmin@goandstudycluster.4jz25t7.mongodb.net/?appName=goandstudycluster',
    tlsAllowInvalidCertificates=True
)
db = client['goandstudydb']

print('=== member_analytics IDs ===')
for doc in db['member_analytics'].find().limit(5):
    doc_id = doc.get('id') or str(doc.get('_id'))
    print(f'  id={doc_id}  type={type(doc_id).__name__}')

print('\n=== member IDs ===')
for doc in db['members'].find().limit(5):
    print(f'  _id={doc["_id"]}  type={type(doc["_id"]).__name__}')

# Check intersection
ma_ids = set(str(doc.get('id', doc.get('_id'))) for doc in db['member_analytics'].find())
m_ids = set(str(doc['_id']) for doc in db['members'].find())
print(f'\nmember_analytics count: {len(ma_ids)}')
print(f'members count: {len(m_ids)}')
print(f'Intersection: {len(ma_ids & m_ids)}')

# Check activity_logs actorId values
print('\n=== activity_logs actorId samples ===')
for doc in db['activity_logs'].find().limit(5):
    print(f'  actorId={doc.get("actorId")}  eventType={doc.get("eventType")}  targetCollection={doc.get("targetCollection")}')
