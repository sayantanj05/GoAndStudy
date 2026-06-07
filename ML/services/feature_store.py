"""Redis-backed feature store with ClickHouse fallback."""
import json
import logging
from typing import Dict, Optional
from datetime import datetime

try:
    import redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False

from scripts.data_loader import get_client as get_ch_client

logger = logging.getLogger(__name__)

_redis_client = None

def get_redis():
    global _redis_client
    if _redis_client is None and REDIS_AVAILABLE:
        _redis_client = redis.Redis(
            host='localhost', port=6379, db=0,
            decode_responses=True, socket_connect_timeout=2
        )
        try:
            _redis_client.ping()
            logger.info("Redis connected successfully")
        except Exception as e:
            logger.warning(f"Redis unavailable, falling back to ClickHouse: {e}")
            _redis_client = None
    return _redis_client


class RedisFeatureStore:
    """Hybrid feature store: Redis (hot) -> ClickHouse (warm)."""

    def __init__(self):
        self.redis = get_redis()
        self.ch = get_ch_client()

    def _cache_key(self, prefix: str, identifier: str) -> str:
        return f"{prefix}:{identifier}"

    def _get_cached(self, key: str) -> Optional[Dict]:
        if not self.redis:
            return None
        try:
            val = self.redis.get(key)
            if val:
                return json.loads(val)
        except Exception as e:
            logger.warning(f"Redis get error for {key}: {e}")
        return None

    def _set_cached(self, key: str, value: Dict, ttl_seconds: int):
        if not self.redis:
            return
        try:
            self.redis.setex(key, ttl_seconds, json.dumps(value, default=str))
        except Exception as e:
            logger.warning(f"Redis set error for {key}: {e}")

    def get_user_features(self, user_id: str) -> Dict:
        key = self._cache_key("user", user_id)
        cached = self._get_cached(key)
        if cached:
            return cached
        features = self._compute_user_features_from_ch(user_id)
        self._set_cached(key, features, 300)
        return features

    def get_book_features(self, book_id: str) -> Dict:
        key = self._cache_key("book", book_id)
        cached = self._get_cached(key)
        if cached:
            return cached
        features = self._compute_book_features_from_ch(book_id)
        self._set_cached(key, features, 600)
        return features

    def _compute_user_features_from_ch(self, user_id: str) -> Dict:
        try:
            query = """SELECT countDistinct(loan_id) as recent_loans,
                countDistinct(CASE WHEN status = 'OVERDUE' THEN loan_id END) as recent_overdues,
                avg(fine_amount) as avg_fine, max(issued_at) as last_loan_date
                FROM fact_loan WHERE user_id = %(user_id)s AND issued_at >= today() - 30"""
            result = self.ch.query(query, parameters={'user_id': user_id})
            recent_row = result.result_rows[0] if result.result_rows else [0, 0, 0, None]

            query2 = """SELECT count(*) as total_ratings, avg(rating) as avg_rating,
                stddevPop(rating) as rating_std, max(created_at) as last_rating_date
                FROM fact_ratings WHERE user_id = %(user_id)s"""
            result2 = self.ch.query(query2, parameters={'user_id': user_id})
            rating_row = result2.result_rows[0] if result2.result_rows else [0, 0, 0, None]

            query3 = """SELECT count(*) as total_searches, countDistinct(query) as unique_searches,
                max(created_at) as last_search_date FROM fact_search
                WHERE user_id = %(user_id)s AND created_at >= today() - 30"""
            result3 = self.ch.query(query3, parameters={'user_id': user_id})
            search_row = result3.result_rows[0] if result3.result_rows else [0, 0, None]

            query4 = """SELECT b.category_ids[1] as genre, count(*) as genre_count
                FROM fact_loan l JOIN dim_book b ON l.book_id = b.book_id
                WHERE l.user_id = %(user_id)s GROUP BY genre ORDER BY genre_count DESC"""
            result4 = self.ch.query(query4, parameters={'user_id': user_id})
            genre_prefs = {row[0]: row[1] for row in result4.result_rows} if result4.result_rows else {}

            last_activity = max(
                recent_row[3] or datetime(2000, 1, 1),
                rating_row[3] or datetime(2000, 1, 1),
                search_row[2] or datetime(2000, 1, 1)
            )
            days_since_active = (datetime.now() - last_activity).days if isinstance(last_activity, datetime) else 999

            return {
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
                    int(recent_row[0] or 0), int(search_row[0] or 0),
                    int(rating_row[0] or 0), days_since_active
                ),
                'computed_at': datetime.now().isoformat()
            }
        except Exception as e:
            logger.error(f"Error computing user features for {user_id}: {e}")
            return {
                'user_id': user_id, 'recent_loans': 0, 'recent_overdues': 0,
                'avg_fine': 0, 'total_ratings': 0, 'avg_rating': 0,
                'rating_std': 0, 'recent_searches': 0, 'unique_searches': 0,
                'genre_preferences': {}, 'days_since_active': 999,
                'engagement_score': 0, 'computed_at': datetime.now().isoformat(),
                'error': str(e)
            }

    def _compute_book_features_from_ch(self, book_id: str) -> Dict:
        try:
            query = """SELECT countDistinct(loan_id) as recent_loans,
                countDistinct(user_id) as unique_borrowers, avg(fine_amount) as avg_fine
                FROM fact_loan WHERE book_id = %(book_id)s AND issued_at >= today() - 30"""
            result = self.ch.query(query, parameters={'book_id': book_id})
            loan_row = result.result_rows[0] if result.result_rows else [0, 0, 0]

            query2 = """SELECT count(*) as total_ratings, avg(rating) as avg_rating,
                count(CASE WHEN rating >= 4 THEN 1 END) as high_ratings
                FROM fact_ratings WHERE book_id = %(book_id)s"""
            result2 = self.ch.query(query2, parameters={'book_id': book_id})
            rating_row = result2.result_rows[0] if result2.result_rows else [0, 0, 0]

            query3 = """SELECT count(*) as wishlist_count FROM fact_wishlist WHERE book_id = %(book_id)s"""
            result3 = self.ch.query(query3, parameters={'book_id': book_id})
            wishlist_count = result3.result_rows[0][0] if result3.result_rows else 0

            return {
                'book_id': book_id,
                'recent_loans': int(loan_row[0] or 0),
                'unique_borrowers': int(loan_row[1] or 0),
                'avg_fine': float(loan_row[2] or 0),
                'total_ratings': int(rating_row[0] or 0),
                'avg_rating': float(rating_row[1] or 0),
                'high_rating_ratio': float(rating_row[2] / rating_row[0]) if rating_row[0] else 0,
                'wishlist_count': int(wishlist_count or 0),
                'trending_score': self._compute_trending_score(
                    int(loan_row[0] or 0), int(rating_row[0] or 0),
                    float(rating_row[1] or 0), int(wishlist_count or 0)
                ),
                'computed_at': datetime.now().isoformat()
            }
        except Exception as e:
            logger.error(f"Error computing book features for {book_id}: {e}")
            return {
                'book_id': book_id, 'recent_loans': 0, 'unique_borrowers': 0,
                'avg_fine': 0, 'total_ratings': 0, 'avg_rating': 0,
                'high_rating_ratio': 0, 'wishlist_count': 0,
                'trending_score': 0, 'computed_at': datetime.now().isoformat(),
                'error': str(e)
            }

    @staticmethod
    def _compute_engagement_score(loans: int, searches: int, ratings: int, days_inactive: int) -> float:
        recency_penalty = max(0, 1 - days_inactive / 30)
        activity_score = min(100, (loans * 10 + searches * 3 + ratings * 5))
        return round(activity_score * recency_penalty, 2)

    @staticmethod
    def _compute_trending_score(loans: int, ratings: int, avg_rating: float, wishlist: int) -> float:
        rating_boost = avg_rating / 5.0 if avg_rating else 0.5
        return round((loans * 3 + ratings * 2 + wishlist * 1.5) * rating_boost, 2)
