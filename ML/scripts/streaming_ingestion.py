"""Streaming interaction ingestion for real-time ML updates."""
import os
import json
import logging
import threading
from datetime import datetime
from typing import Dict, List
from collections import deque

import pandas as pd

logger = logging.getLogger(__name__)

# In-memory buffer for recent interactions (before batch write to ClickHouse)
_INTERACTION_BUFFER = deque(maxlen=10000)
_BUFFER_LOCK = threading.Lock()

# Interaction types
INTERACTION_LOAN = 'loan'
INTERACTION_RETURN = 'return'
INTERACTION_RATING = 'rating'
INTERACTION_SEARCH = 'search'
INTERACTION_WISHLIST = 'wishlist'


class InteractionEvent:
    """A single user interaction event."""
    
    def __init__(self, user_id: str, book_id: str, interaction_type: str,
                 rating: float = None, timestamp: datetime = None,
                 metadata: Dict = None):
        self.user_id = user_id
        self.book_id = book_id
        self.interaction_type = interaction_type
        self.rating = rating
        self.timestamp = timestamp or datetime.now()
        self.metadata = metadata or {}
    
    def to_dict(self) -> Dict:
        return {
            'user_id': self.user_id,
            'book_id': self.book_id,
            'interaction_type': self.interaction_type,
            'rating': self.rating,
            'timestamp': self.timestamp.isoformat(),
            'metadata': self.metadata
        }
    
    @classmethod
    def from_dict(cls, data: Dict):
        return cls(
            user_id=data['user_id'],
            book_id=data['book_id'],
            interaction_type=data['interaction_type'],
            rating=data.get('rating'),
            timestamp=datetime.fromisoformat(data['timestamp']),
            metadata=data.get('metadata', {})
        )


class StreamingIngestion:
    """Handles real-time interaction ingestion with buffering."""
    
    def __init__(self, batch_size: int = 100, flush_interval_seconds: int = 60):
        self.batch_size = batch_size
        self.flush_interval = flush_interval_seconds
        self._flush_timer = None
        self._start_flush_timer()
    
    def _start_flush_timer(self):
        """Start periodic flush timer."""
        self._flush_buffer()
        self._flush_timer = threading.Timer(self.flush_interval, self._start_flush_timer)
        self._flush_timer.daemon = True
        self._flush_timer.start()
    
    def ingest(self, event: InteractionEvent):
        """Ingest a single interaction event."""
        with _BUFFER_LOCK:
            _INTERACTION_BUFFER.append(event)
        
        # Flush if buffer is full
        if len(_INTERACTION_BUFFER) >= self.batch_size:
            self._flush_buffer()
        
        logger.debug(f"Ingested: {event.interaction_type} user={event.user_id} book={event.book_id}")
    
    def ingest_loan(self, user_id: str, book_id: str, metadata: Dict = None):
        """Record a loan event."""
        self.ingest(InteractionEvent(
            user_id=user_id, book_id=book_id,
            interaction_type=INTERACTION_LOAN, metadata=metadata
        ))
    
    def ingest_rating(self, user_id: str, book_id: str, rating: float, metadata: Dict = None):
        """Record a rating event."""
        self.ingest(InteractionEvent(
            user_id=user_id, book_id=book_id,
            interaction_type=INTERACTION_RATING, rating=rating, metadata=metadata
        ))
    
    def ingest_search(self, user_id: str, query: str, metadata: Dict = None):
        """Record a search event (book_id is the top result or empty)."""
        self.ingest(InteractionEvent(
            user_id=user_id, book_id=metadata.get('top_result_id', ''),
            interaction_type=INTERACTION_SEARCH, metadata={**metadata, 'query': query}
        ))
    
    def ingest_wishlist(self, user_id: str, book_id: str, metadata: Dict = None):
        """Record a wishlist event."""
        self.ingest(InteractionEvent(
            user_id=user_id, book_id=book_id,
            interaction_type=INTERACTION_WISHLIST, metadata=metadata
        ))
    
    def _flush_buffer(self):
        """Flush buffered interactions to ClickHouse."""
        with _BUFFER_LOCK:
            if len(_INTERACTION_BUFFER) == 0:
                return
            batch = list(_INTERACTION_BUFFER)
            _INTERACTION_BUFFER.clear()
        
        try:
            self._write_to_clickhouse(batch)
            self._write_to_local_log(batch)
            logger.info(f"Flushed {len(batch)} interactions to ClickHouse")
        except Exception as e:
            logger.error(f"Failed to flush interactions: {e}")
            # Re-add to buffer for retry
            with _BUFFER_LOCK:
                for event in batch:
                    _INTERACTION_BUFFER.append(event)
    
    def _write_to_clickhouse(self, batch: List[InteractionEvent]):
        """Write batch to ClickHouse fact tables."""
        try:
            from scripts.data_loader import get_ch_client
            ch = get_ch_client()
            
            # Group by interaction type
            loans = [e for e in batch if e.interaction_type == INTERACTION_LOAN]
            ratings = [e for e in batch if e.interaction_type == INTERACTION_RATING]
            searches = [e for e in batch if e.interaction_type == INTERACTION_SEARCH]
            wishlists = [e for e in batch if e.interaction_type == INTERACTION_WISHLIST]
            
            # Insert loans
            if loans:
                loan_data = [
                    (e.user_id, e.book_id, e.timestamp, 'ACTIVE', 0, 14)
                    for e in loans
                ]
                ch.insert('fact_loan', loan_data,
                         columns=['user_id', 'book_id', 'issued_at', 'status', 'fine_amount', 'max_days'])
            
            # Insert ratings
            if ratings:
                rating_data = [
                    (e.user_id, e.book_id, e.rating, e.timestamp)
                    for e in ratings
                ]
                ch.insert('fact_ratings', rating_data,
                         columns=['user_id', 'book_id', 'rating', 'rated_at'])
            
            # Insert searches
            if searches:
                search_data = [
                    (e.user_id, e.metadata.get('query', ''), e.timestamp, 
                     e.metadata.get('results_count', 0))
                    for e in searches
                ]
                ch.insert('fact_search', search_data,
                         columns=['user_id', 'search_query', 'searched_at', 'results_count'])
            
            # Insert wishlists
            if wishlists:
                wishlist_data = [
                    (e.user_id, e.book_id, e.timestamp)
                    for e in wishlists
                ]
                ch.insert('fact_wishlist', wishlist_data,
                         columns=['user_id', 'book_id', 'added_at'])
            
        except Exception as e:
            raise Exception(f"ClickHouse write failed: {e}")
    
    def _write_to_local_log(self, batch: List[InteractionEvent]):
        """Backup to local JSONL file."""
        log_dir = os.path.join(os.path.dirname(__file__), '..', 'logs')
        os.makedirs(log_dir, exist_ok=True)
        log_file = os.path.join(log_dir, f"interactions_{datetime.now().strftime('%Y%m%d')}.jsonl")
        
        with open(log_file, 'a', encoding='utf-8') as f:
            for event in batch:
                f.write(json.dumps(event.to_dict()) + '\n')
    
    def get_recent_interactions(self, user_id: str = None, n: int = 50) -> List[Dict]:
        """Get recent interactions from buffer."""
        with _BUFFER_LOCK:
            interactions = list(_INTERACTION_BUFFER)
        
        if user_id:
            interactions = [e for e in interactions if e.user_id == user_id]
        
        interactions = sorted(interactions, key=lambda e: e.timestamp, reverse=True)
        return [e.to_dict() for e in interactions[:n]]
    
    def get_buffer_stats(self) -> Dict:
        """Get buffer statistics."""
        with _BUFFER_LOCK:
            return {
                'buffer_size': len(_INTERACTION_BUFFER),
                'buffer_capacity': _INTERACTION_BUFFER.maxlen,
                'buffer_full_ratio': len(_INTERACTION_BUFFER) / _INTERACTION_BUFFER.maxlen
            }
    
    def shutdown(self):
        """Graceful shutdown - flush remaining buffer."""
        if self._flush_timer:
            self._flush_timer.cancel()
        self._flush_buffer()


# Global singleton
_streaming = None

def get_streaming_ingestion():
    global _streaming
    if _streaming is None:
        _streaming = StreamingIngestion()
    return _streaming
