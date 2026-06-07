"""Production re-ranker with diversity, freshness, exploration, and business rules."""
import numpy as np
from collections import defaultdict
from datetime import datetime

from services.mab_explorer import GenreBandit


class ReRanker:
    def __init__(self, genres=None):
        self.bandit = GenreBandit(genres or ['Fiction', 'Sci-Fi', 'History', 'Science', 'Technology', 'Biography', 'Mystery', 'Romance', 'Fantasy'])
        self.max_per_genre = 3
        self.freshness_boost = 1.1
        self.freshness_days = 30
        self.exploration_ratio = 0.1
    
    def rerank(self, scored_items: list, user_id: str, top_n: int = 5, similar_n: int = 10):
        """
        scored_items: list of dicts with keys: book_id, score, genre, days_since_added
        Returns: (top_items, similar_items)
        """
        # 1. Business rule filter: only available books
        available = [item for item in scored_items if item.get('available_copies', 1) > 0]
        
        # 2. Diversity: max N per genre in top pool
        genre_counts = defaultdict(int)
        diverse = []
        for item in available:
            g = item.get('genre', 'unknown')
            if genre_counts[g] < self.max_per_genre:
                diverse.append(item)
                genre_counts[g] += 1
        
        # 3. Freshness boost
        for item in diverse:
            if item.get('days_since_added', 999) < self.freshness_days:
                item['score'] *= self.freshness_boost
        
        # 4. Re-sort after freshness boost
        diverse.sort(key=lambda x: x['score'], reverse=True)
        
        # 5. Exploration: replace bottom 10% with MAB picks
        n_explore = max(1, int(len(diverse) * self.exploration_ratio))
        explore_genres = self.bandit.get_exploration_slots(n_explore, list(genre_counts.keys()))
        
        # Simple implementation: boost score of underrepresented genres
        explore_ids = set()
        for genre in explore_genres:
            for item in scored_items:
                if item.get('genre') == genre and item['book_id'] not in explore_ids:
                    item['score'] *= 1.05  # Small exploration boost
                    explore_ids.add(item['book_id'])
                    break
        
        # Re-sort one final time
        diverse.sort(key=lambda x: x['score'], reverse=True)
        
        # 6. Split into two sections
        top = diverse[:top_n]
        similar = diverse[top_n:top_n + similar_n]
        return top, similar
    
    def record_feedback(self, book_id: str, genre: str, clicked: bool):
        """Update MAB with user feedback."""
        reward = 1.0 if clicked else 0.0
        self.bandit.update(genre, reward)

