"""Real-time feature computation from ClickHouse for online recommendations."""
import os
import time
import logging
from typing import Dict, List, Optional
from datetime import datetime, timedelta

import numpy as np
import pandas as pd

logger = logging.getLogger(__name__)

# Simple in-memory cache with TTL
_feature_cache = {}
_cache_ttl_seconds = 60  # Cache features for 60 seconds


def get_ch_client():
    """Get ClickHouse client."""
    import clickhouse_connect
    return clickhouse_connect.get_client(
        host='localhost', port=8123,
        username='default', password='',
        database='library_dw'
    )


def _cache_key(prefix: str, identifier: str) -> str:
    return f"{prefix}:{identifier}"


def _get_cached(key: str) -> Optional[dict]:
    """Get cached value if not expired."""
    if key in _feature_cache:
        value, timestamp = _feature_cache[key]
        if time.time() - timestamp < _cache_ttl_seconds:
            return value
        else:
            del _feature_cache[key]
    return None


def _set_cached(key: str, value: dict):
    """Cache value with timestamp."""
    _feature_cache[key] = (value, time.time())


class OnlineFeatureStore:
    """Real-time feature store backed by ClickHouse."""
    
    def __init__(self):
        self._ch = None
    
    @property
    def ch(self):
        if self._ch is None:
            self._ch = get_ch_client()
        return self._ch
    
    def get_user_features(self, user_id: str) -> Dict:
        """Compute real-time user features from ClickHouse."""
        cache_key = _cache_key("user", user_id)
        cached = _get_cached(cache_key)
        if cached:
            return cached
        
        try:
            # Recent activity (last 30 days)
            query = """
                SELECT 
                    countDistinct(loan_id) as recent_loans,
                    countDistinct(CASE WHEN status = 'OVERDUE' THEN loan_id END) as recent_overdues,
                    avg(fine_amount) as avg_fine,
                    max(issued_at) as last_loan_date
                FROM fact_loan
                WHERE user_id = %(user_id)s
                  AND issued_at >= today() - 30
            """
            result = self.ch.query(query, parameters={'user_id': user_id})
            recent_row = result.result_rows[0] if result.result_rows else [0, 0, 0, None]
            
            # Rating behavior
            query2 = """
                SELECT 
                    count(*) as total_ratings,
                    avg(rating) as avg_rating,
                    stddevPop(rating) as rating_std,
                    max(rated_at) as last_rating_date
                FROM fact_ratings
                WHERE user_id = %(user_id)s
            """
            result2 = self.ch.query(query2, parameters={'user_id': user_id})
            rating_row = result2.result_rows[0] if result2.result_rows else [0, 0, 0, None]
            
            # Search behavior
            query3 = """
                SELECT 
                    count(*) as total_searches,
                    countDistinct(search_query) as unique_searches,
                    max(searched_at) as last_search_date
                FROM fact_search
                WHERE user_id = %(user_id)s
                  AND searched_at >= today() - 30
            """
            result3 = self.ch.query(query3, parameters={'user_id': user_id})
            search_row = result3.result_rows[0] if result3.result_rows else [0, 0, None]
            
            # Genre preferences from loans
            query4 = """
                SELECT 
                    b.genre,
                    count(*) as genre_count
                FROM fact_loan l
                JOIN dim_book b ON l.book_id = b.book_id
                WHERE l.user_id = %(user_id)s
                GROUP BY b.genre
                ORDER BY genre_count DESC
            """
            result4 = self.ch.query(query4, parameters={'user_id': user_id})
            genre_prefs = {row[0]: row[1] for row in result4.result_rows} if result4.result_rows else {}
            
            # Time since last activity
            last_activity = max(
                recent_row[3] or datetime(2000, 1, 1),
                rating_row[3] or datetime(2000, 1, 1),
                search_row[2] or datetime(2000, 1, 1)
            )
            days_since_active = (datetime.now() - last_activity).days if isinstance(last_activity, datetime) else 999
            
            features = {
                'user_id': user_id,
                'recent_loans': int(recent_row[0] or 0),
                'recent_overdues': int(recent_row[1] or 0),
                'avg_fine': float(recent_row[2] or 0),
                'total_ratings': int(rating_row[0] or 0),
                'avg_rating': float(rating_row[1] or 0),
                'rating_std': float(rating_row[2] or 0),
                'recent_searches': int(search_row[0] or 0),
                'unique_searches': int(search_row[1] or 0),
                'genre_preferences': genre_prefs,
                'days_since_active': days_since_active,
                'engagement_score': self._compute_engagement_score(
                    int(recent_row[0] or 0),
                    int(search_row[0] or 0),
                    int(rating_row[0] or 0),
                    days_since_active
                ),
                'computed_at': datetime.now().isoformat()
            }
            
            _set_cached(cache_key, features)
            return features
            
        except Exception as e:
            logger.error(f"Error computing user features for {user_id}: {e}")
            return {
                'user_id': user_id,
                'recent_loans': 0,
                'recent_overdues': 0,
                'avg_fine': 0,
                'total_ratings': 0,
                'avg_rating': 0,
                'rating_std': 0,
                'recent_searches': 0,
                'unique_searches': 0,
                'genre_preferences': {},
                'days_since_active': 999,
                'engagement_score': 0,
                'computed_at': datetime.now().isoformat(),
                'error': str(e)
            }
    
    def get_book_features(self, book_id: str) -> Dict:
        """Compute real-time book features from ClickHouse."""
        cache_key = _cache_key("book", book_id)
        cached = _get_cached(cache_key)
        if cached:
            return cached
        
        try:
            # Recent popularity
            query = """
                SELECT 
                    countDistinct(loan_id) as recent_loans,
                    countDistinct(user_id) as unique_borrowers,
                    avg(fine_amount) as avg_fine
                FROM fact_loan
                WHERE book_id = %(book_id)s
                  AND issued_at >= today() - 30
            """
            result = self.ch.query(query, parameters={'book_id': book_id})
            loan_row = result.result_rows[0] if result.result_rows else [0, 0, 0]
            
            # Ratings
            query2 = """
                SELECT 
                    count(*) as total_ratings,
                    avg(rating) as avg_rating,
                    count(CASE WHEN rating >= 4 THEN 1 END) as high_ratings
                FROM fact_ratings
                WHERE book_id = %(book_id)s
            """
            result2 = self.ch.query(query2, parameters={'book_id': book_id})
            rating_row = result2.result_rows[0] if result2.result_rows else [0, 0, 0]
            
            # Wishlist count
            query3 = """
                SELECT count(*) as wishlist_count
                FROM fact_wishlist
                WHERE book_id = %(book_id)s
            """
            result3 = self.ch.query(query3, parameters={'book_id': book_id})
            wishlist_count = result3.result_rows[0][0] if result3.result_rows else 0
            
            features = {
                'book_id': book_id,
                'recent_loans': int(loan_row[0] or 0),
                'unique_borrowers': int(loan_row[1] or 0),
                'avg_fine': float(loan_row[2] or 0),
                'total_ratings': int(rating_row[0] or 0),
                'avg_rating': float(rating_row[1] or 0),
                'high_rating_ratio': float(rating_row[2] / rating_row[0]) if rating_row[0] else 0,
                'wishlist_count': int(wishlist_count or 0),
                'trending_score': self._compute_trending_score(
                    int(loan_row[0] or 0),
                    int(rating_row[0] or 0),
                    float(rating_row[1] or 0),
                    int(wishlist_count or 0)
                ),
                'computed_at': datetime.now().isoformat()
            }
            
            _set_cached(cache_key, features)
            return features
            
        except Exception as e:
            logger.error(f"Error computing book features for {book_id}: {e}")
            return {
                'book_id': book_id,
                'recent_loans': 0,
                'unique_borrowers': 0,
                'avg_fine': 0,
                'total_ratings': 0,
                'avg_rating': 0,
                'high_rating_ratio': 0,
                'wishlist_count': 0,
                'trending_score': 0,
                'computed_at': datetime.now().isoformat(),
                'error': str(e)
            }
    
    def get_trending_books(self, n: int = 10, days: int = 7) -> List[Dict]:
        """Get trending books based on recent activity."""
        cache_key = _cache_key("trending", f"{n}_{days}")
        cached = _get_cached(cache_key)
        if cached:
            return cached.get('books', [])
        
        try:
            query = """
                SELECT 
                    b.book_id,
                    b.title,
                    b.author,
                    b.genre,
                    countDistinct(l.loan_id) as loan_count,
                    countDistinct(r.review_id) as rating_count,
                    avg(r.rating) as avg_rating,
                    countDistinct(w.wishlist_id) as wishlist_count
                FROM dim_book b
                LEFT JOIN fact_loan l ON b.book_id = l.book_id 
                    AND l.issued_at >= today() - %(days)s
                LEFT JOIN fact_ratings r ON b.book_id = r.book_id
                LEFT JOIN fact_wishlist w ON b.book_id = w.book_id
                GROUP BY b.book_id, b.title, b.author, b.genre
                HAVING loan_count > 0 OR rating_count > 0
                ORDER BY (loan_count * 3 + rating_count * 2 + wishlist_count) DESC
                LIMIT %(n)s
            """
            result = self.ch.query(query, parameters={'n': n, 'days': days})
            
            books = []
            for row in result.result_rows:
                books.append({
                    'book_id': row[0],
                    'title': row[1],
                    'author': row[2],
                    'genre': row[3],
                    'recent_loans': int(row[4] or 0),
                    'rating_count': int(row[5] or 0),
                    'avg_rating': float(row[6] or 0),
                    'wishlist_count': int(row[7] or 0),
                    'trending_score': int(row[4] or 0) * 3 + int(row[5] or 0) * 2 + int(row[7] or 0)
                })
            
            _set_cached(cache_key, {'books': books})
            return books
            
        except Exception as e:
            logger.error(f"Error getting trending books: {e}")
            return []
    
    def get_similar_users(self, user_id: str, n: int = 5) -> List[Dict]:
        """Find users with similar borrowing patterns."""
        try:
            query = """
                SELECT 
                    l2.user_id,
                    d.name,
                    count(DISTINCT l1.book_id) as common_books
                FROM fact_loan l1
                JOIN fact_loan l2 ON l1.book_id = l2.book_id AND l1.user_id != l2.user_id
                JOIN dim_user d ON l2.user_id = d.user_id
                WHERE l1.user_id = %(user_id)s
                GROUP BY l2.user_id, d.name
                ORDER BY common_books DESC
                LIMIT %(n)s
            """
            result = self.ch.query(query, parameters={'user_id': user_id, 'n': n})
            
            users = []
            for row in result.result_rows:
                users.append({
                    'user_id': row[0],
                    'name': row[1],
                    'common_books': int(row[2])
                })
            return users
            
        except Exception as e:
            logger.error(f"Error finding similar users: {e}")
            return []
    
    def _compute_engagement_score(self, loans: int, searches: int, ratings: int, days_inactive: int) -> float:
        """Compute user engagement score (0-100)."""
        recency_penalty = max(0, 1 - days_inactive / 30)  # Decay over 30 days
        activity_score = min(100, (loans * 10 + searches * 3 + ratings * 5))
        return round(activity_score * recency_penalty, 2)
    
    def _compute_trending_score(self, loans: int, ratings: int, avg_rating: float, wishlist: int) -> float:
        """Compute book trending score."""
        rating_boost = avg_rating / 5.0 if avg_rating else 0.5
        return round((loans * 3 + ratings * 2 + wishlist * 1.5) * rating_boost, 2)


# Global singleton
_feature_store = None

def get_feature_store():
    global _feature_store
    if _feature_store is None:
        _feature_store = OnlineFeatureStore()
    return _feature_store
