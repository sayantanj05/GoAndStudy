"""
MongoDB data extraction module
"""
import os
from typing import List, Dict, Any
from datetime import datetime
from bson import ObjectId
import pandas as pd
from config.database import mongo_conn

class MongoExtractor:
    """Extract data from MongoDB collections"""
    
    def __init__(self):
        self.batch_size = int(os.getenv('BATCH_SIZE', 1000))
    
    def extract_collection(self, collection_name: str, query: Dict = None, 
                          batch_mode: bool = True) -> List[Dict]:
        """Extract data from a MongoDB collection"""
        try:
            collection = mongo_conn.get_collection(collection_name)
            
            if collection is None:
                print(f"❌ Collection {collection_name} not found")
                return []
            
            if query is None:
                query = {}
            
            if batch_mode:
                # Process in batches for large collections
                cursor = collection.find(query).batch_size(self.batch_size)
                documents = list(cursor)
            else:
                documents = list(collection.find(query))
            
            print(f"📥 Extracted {len(documents)} documents from {collection_name}")
            return documents
            
        except Exception as e:
            print(f"❌ Error extracting from {collection_name}: {e}")
            return []
    
    def convert_objectid_to_string(self, documents: List[Dict]) -> List[Dict]:
        """Convert ObjectId fields to strings for ClickHouse compatibility"""
        converted_docs = []
        
        for doc in documents:
            converted_doc = {}
            for key, value in doc.items():
                if isinstance(value, ObjectId):
                    converted_doc[key] = str(value)
                elif isinstance(value, dict):
                    # Handle nested objects
                    converted_doc[key] = self._convert_nested_objectid(value)
                elif isinstance(value, list):
                    # Handle lists with potential ObjectIds
                    converted_doc[key] = self._convert_list_objectid(value)
                else:
                    converted_doc[key] = value
            converted_docs.append(converted_doc)
        
        return converted_docs
    
    def _convert_nested_objectid(self, obj: Dict) -> Dict:
        """Convert ObjectIds in nested dictionaries"""
        converted = {}
        for key, value in obj.items():
            if isinstance(value, ObjectId):
                converted[key] = str(value)
            elif isinstance(value, dict):
                converted[key] = self._convert_nested_objectid(value)
            elif isinstance(value, list):
                converted[key] = self._convert_list_objectid(value)
            else:
                converted[key] = value
        return converted
    
    def _convert_list_objectid(self, lst: List) -> List:
        """Convert ObjectIds in lists"""
        converted = []
        for item in lst:
            if isinstance(item, ObjectId):
                converted.append(str(item))
            elif isinstance(item, dict):
                converted.append(self._convert_nested_objectid(item))
            elif isinstance(item, list):
                converted.append(self._convert_list_objectid(item))
            else:
                converted.append(item)
        return converted
    
    def extract_all_collections(self) -> Dict[str, List[Dict]]:
        """Extract data from all relevant MongoDB collections"""
        collections_map = {
            # Dimension tables
            'dim_user': 'members',
            'dim_book': 'books',
            'dim_author': 'authors',
            'dim_category': 'book_categories',
            'dim_publisher': 'publishers',
            'dim_member_preferences': 'member_preferences',
            
            # Fact tables
            'fact_loan': 'loans',
            'fact_payment': 'fine_records',
            'fact_ratings': 'book_reviews',
            'fact_wishlist': 'wishlists',
            'fact_search': 'search_logs',
            'fact_reading_session': 'reading_sessions',
            'fact_reservation': 'reservations',
            'fact_activity_log': 'activity_logs',
            'fact_book_embedding': 'book_embeddings',
            'fact_member_analytics': 'member_analytics',
            'fact_chatbot_feedback': 'chatbot_feedback',
            'fact_recommendation': 'recommendation_feedback',
            'fact_events': 'loan_events',
            'fact_book_request': 'book_requests',
            'fact_notifications': 'notifications',
            'fact_ai_logs': 'ai_logs',
            'fact_chatbot_sessions': 'chatbot_sessions',
            'fact_genre_analytics': 'genre_analytics',
            'fact_admins': 'admins',
            'fact_staff': 'staff',
            'fact_roles': 'roles',
            'fact_system_config': 'system_config'
        }
        
        extracted_data = {}
        
        for table_name, collection_name in collections_map.items():
            print(f"\n🔄 Extracting {collection_name} -> {table_name}")
            
            # Special handling for ActivityLog (need to separate search from other activities)
            if collection_name == 'ActivityLog' and table_name == 'fact_search':
                documents = self.extract_collection(collection_name, 
                                                  {'action': {'$regex': 'search', '$options': 'i'}})
            elif collection_name == 'ActivityLog' and table_name == 'fact_activity_log':
                documents = self.extract_collection(collection_name, 
                                                  {'action': {'$not': {'$regex': 'search', '$options': 'i'}}})
            else:
                documents = self.extract_collection(collection_name)
            
            if documents:
                # Convert ObjectIds to strings
                converted_docs = self.convert_objectid_to_string(documents)
                extracted_data[table_name] = converted_docs
            else:
                print(f"⚠️  No data found for {collection_name}")
        
        return extracted_data
