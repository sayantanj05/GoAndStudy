"""KnowledgeBase implementation."""
import uuid
from datetime import datetime
from typing import Dict, Any
import pymongo

class KnowledgeBase:
    """Knowledge base abstraction."""
    
    def __init__(self, mongo_db):
        self.db = mongo_db
        self.documents_collection = self.db["knowledge_documents"]
    
    def add_document(self, title: str, content: str, doc_type: str = "general", 
                    metadata: Dict[str, Any] = None) -> Dict[str, Any]:
        """Add a document to the knowledge base."""
        doc = {
            "_id": str(uuid.uuid4()),
            "title": title,
            "content": content,
            "doc_type": doc_type,
            "metadata": metadata or {},
            "created_at": datetime.utcnow(),
            "updated_at": datetime.utcnow()
        }
        self.documents_collection.insert_one(doc)
        return doc
