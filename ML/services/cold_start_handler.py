"""Cold-start handler for new users and new books."""
import random
from datetime import datetime

class ColdStartHandler:
    """Handles recommendations for users and items with no interaction history."""
    
    def __init__(self, feature_store):
        self.feature_store = feature_store
        self.popular_books = []
        self.trending_books = []
        self._refresh_popular()
    
    def _refresh_popular(self):
        """Refresh popular/trending book lists from ClickHouse."""
        try:
            # Get trending books
            query = """
                SELECT book_id, count(*) as loan_count, avg(rating) as avg_rating
                FROM fact_loan l LEFT JOIN fact_ratings r ON l.book_id = r.book_id
                WHERE l.issued_at >= today() - 30
                GROUP BY book_id
                ORDER BY loan_count DESC
                LIMIT 100
            """
            result = self.feature_store.ch.query(query)
            self.trending_books = [row[0] for row in result.result_rows]
            
            # Get all-time popular
            query2 = """
                SELECT book_id, count(*) as total_loans
                FROM fact_loan
                GROUP BY book_id
                ORDER BY total_loans DESC
                LIMIT 100
            """
            result2 = self.feature_store.ch.query(query2)
            self.popular_books = [row[0] for row in result2.result_rows]
        except Exception:
            pass
    
    def get_recommendations(self, user_id: str, n: int = 10, exploration_rate: float = 0.3):
        """
        For new users: mix popular, trending, and diverse genres.
        exploration_rate: fraction of slots for genre-diverse exploration
        """
        if not self.popular_books:
            self._refresh_popular()
        
        n_explore = int(n * exploration_rate)
        n_popular = n - n_explore
        
        # Popular picks
        popular = self.popular_books[:n_popular]
        
        # Exploration: random trending not in popular
        explore_pool = [b for b in self.trending_books if b not in popular]
        explore = random.sample(explore_pool, min(n_explore, len(explore_pool))) if explore_pool else []
        
        recommendations = popular + explore
        
        # Build response items
        items = []
        for book_id in recommendations[:n]:
            book_feats = self.feature_store.get_book_features(book_id)
            items.append({
                'book_id': book_id,
                'score': 0.0,  # No personalized score for cold start
                'genre': book_feats.get('genre', 'unknown'),
                'reason': 'popular' if book_id in popular else 'trending_explore',
                'available_copies': book_feats.get('available_copies', 1),
                'days_since_added': book_feats.get('days_since_added', 999)
            })
        
        return items
    
    def is_cold_user(self, user_id: str) -> bool:
        """Check if user has insufficient history."""
        user_feats = self.feature_store.get_user_features(user_id)
        total_loans = user_feats.get('recent_loans', 0)
        total_ratings = user_feats.get('total_ratings', 0)
        return (total_loans + total_ratings) < 3
    
    def is_cold_book(self, book_id: str) -> bool:
        """Check if book has insufficient interactions."""
        book_feats = self.feature_store.get_book_features(book_id)
        return book_feats.get('recent_loans', 0) < 2

