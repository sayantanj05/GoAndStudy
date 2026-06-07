#!/usr/bin/env python3
"""
Check staff credentials in database
"""

import pymongo
from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()
client = MongoClient(os.getenv('MONGODB_URI'))
db = client.goandstudy

# Check staff credentials
staff = list(db.staff.find({}))
print('Staff accounts in database:')
for s in staff:
    print(f'  Email: {s["email"]}, Role: {s["role"]}, Active: {s["isActive"]}')
    print(f'  Password Hash: {s["passwordHash"][:50]}...')

client.close()
