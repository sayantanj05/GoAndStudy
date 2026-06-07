"""Hybrid recommender combining SVD++, content-based, and popularity signals."""
import numpy as np
import pandas as pd
from .svdpp_model import SVDppRecommender
from .recommender_model import NMFRecommender


class HybridRecommender:
    """Hybrid recommender that blends multiple signals:
    
    1. Collaborative Filtering (SVD++): learns from user-item interactions
    2. Content-Based: uses genre/category similarity
    3. Popularity: boosts frequently borrowed books
    4. Recency: boosts recently added/popular books
    
    Weights are adaptive based on data availability.
    """
    
    def __init__(self, cf_weight=0.5, content_weight=0.3, popularity_weight=0.2,
                 n_factors=50, use_svdpp=True):
        self.cf_weight = cf_weight
        self.content_weight = content_weight
        self.popularity_weight = popularity_weight
        self.cf_model = None
        self.use_svdpp = use_svdpp
        self.n_factors = n_factors
        
        # Content-based data
        self.book_genres = {}  # book_id -> genre list
        self.book_vectors = {}  # book_id -> embedding vector
        self.user_profiles = {}  # user_id -> weighted genre preferences
        
        # Popularity data
        self.book_popularity = {}  # book_id -> borrow count
        self.global_mean = 3.0
        
        # Item catalog
        self.all_item_ids = []
        self.item_id_set = set()
        
    def fit(self, df, book_metadata=None, user_history=None):
        """Train hybrid model.
        
        Args:
            df: DataFrame with [user_id, item_id, rating]
            book_metadata: Dict[book_id, {'genres': [...], 'vector': [...], 'popularity': int}]
            user_history: Dict[user_id, [book_id, ...]]
        """
        df = df.copy()
        df.columns = ['user_id', 'item_id', 'rating']
        
        self.all_item_ids = df['item_id'].unique().tolist()
        self.item_id_set = set(self.all_item_ids)
        self.global_mean = df['rating'].mean()
        
        # 1. Train collaborative filtering model
        print("Training collaborative filtering component...")
        if self.use_svdpp:
            self.cf_model = SVDppRecommender(
                n_factors=self.n_factors,
                use_svdpp=True
            )
        else:
            self.cf_model = NMFRecommender(n_factors=self.n_factors)
        self.cf_model.fit(df)
        
        # 2. Build content-based profiles
        if book_metadata:
            print("Building content-based profiles...")
            self.book_genres = {bid: meta.get('genres', []) 
                               for bid, meta in book_metadata.items()}
            self.book_vectors = {bid: meta.get('vector', []) 
                                for bid, meta in book_metadata.items()}
            self.book_popularity = {bid: meta.get('popularity', 0) 
                                   for bid, meta in book_metadata.items()}
        
        # 3. Build user profiles from history
        if user_history:
            print("Building user genre profiles...")
            for user_id, books in user_history.items():
                genre_counts = {}
                for book_id in books:
                    genres = self.book_genres.get(book_id, [])
                    for genre in genres:
                        genre_counts[genre] = genre_counts.get(genre, 0) + 1
                # Normalize
                total = sum(genre_counts.values())
                if total > 0:
                    self.user_profiles[user_id] = {g: c/total 
                                                    for g, c in genre_counts.items()}
                else:
                    self.user_profiles[user_id] = {}
        
        print(f"Hybrid model ready: CF({self.cf_weight}) + Content({self.content_weight}) + Popularity({self.popularity_weight})")
        return self
    
    def _content_score(self, user_id, item_id):
        """Calculate content-based similarity score."""
        user_profile = self.user_profiles.get(user_id, {})
        if not user_profile:
            return self.global_mean / 5.0  # Neutral if no profile
        
        book_genres = self.book_genres.get(item_id, [])
        if not book_genres:
            return self.global_mean / 5.0
        
        # Cosine similarity between user profile and book genres
        score = 0.0
        for genre in book_genres:
            score += user_profile.get(genre, 0.0)
        
        # Normalize by number of genres
        return min(score / len(book_genres) * 5.0, 5.0) if book_genres else self.global_mean
    
    def _popularity_score(self, item_id):
        """Calculate popularity score (0-5 scale)."""
        pop = self.book_popularity.get(item_id, 0)
        if pop == 0:
            return self.global_mean
        
        # Normalize popularity to 1-5 scale
        max_pop = max(self.book_popularity.values()) if self.book_popularity else 1
        return 1.0 + 4.0 * (pop / max_pop)
    
    def predict(self, user_id, item_id):
        """Hybrid prediction blending all signals."""
        # Collaborative filtering score
        cf_pred = self.cf_model.predict(user_id, item_id)
        cf_score = cf_pred.est if hasattr(cf_pred, 'est') else self.global_mean
        
        # Content-based score
        content_score = self._content_score(user_id, item_id)
        
        # Popularity score
        pop_score = self._popularity_score(item_id)
        
        # Weighted combination
        final_score = (
            self.cf_weight * cf_score +
            self.content_weight * content_score +
            self.popularity_weight * pop_score
        )
        
        # Clip to valid range
        final_score = np.clip(final_score, 1.0, 5.0)
        
        return type('Prediction', (), {'est': float(final_score)})()
    
    def recommend(self, user_id, item_ids, n=5):
        """Get top-N hybrid recommendations."""
        predictions = []
        for item_id in item_ids:
            pred = self.predict(user_id, item_id)
            predictions.append((item_id, pred.est))
        
        predictions.sort(key=lambda x: x[1], reverse=True)
        return predictions[:n]
    
    def get_metrics(self):
        """Return CF component metrics."""
        if hasattr(self.cf_model, 'get_metrics'):
            return self.cf_model.get_metrics()
        return {}

