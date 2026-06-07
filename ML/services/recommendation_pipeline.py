"""Unified FAANG-level recommendation pipeline.

Orchestrates:
1. Candidate Generation (FAISS + Two-Tower)
2. Feature Engineering (47 features)
3. Ranking (XGBoost LambdaRank)
4. Re-Ranking (Diversity + Freshness + MAB)
5. Cold Start Handling
6. A/B Testing via ModelRegistryV2
7. Logging & Monitoring
"""
import time
import logging
from typing import List, Dict, Tuple
from datetime import datetime

from services.feature_store import RedisFeatureStore
from services.candidate_generator import FaissCandidateGenerator
from services.ranker import XGBoostRanker
from services.reranker import ReRanker
from services.cold_start_handler import ColdStartHandler
from services.session_model import SessionIntentModel
from api.model_registry_v2 import get_registry_v2

logger = logging.getLogger(__name__)


class RecommendationPipeline:
    """End-to-end recommendation pipeline for production serving."""
    
    def __init__(self):
        self.feature_store = RedisFeatureStore()
        self.candidate_gen = FaissCandidateGenerator()
        self.ranker = XGBoostRanker()
        self.reranker = ReRanker()
        self.cold_start = ColdStartHandler(self.feature_store)
        self.session_model = SessionIntentModel()
        self.registry = get_registry_v2()
        
        # Configuration
        self.candidate_k = 200
        self.top_n = 5
        self.similar_n = 10
        self.latency_budget_ms = 100
    
    def recommend(self, user_id: str, context: Dict = None) -> Tuple[List[Dict], List[Dict], Dict]:
        """
        Full recommendation pipeline.
        
        Returns:
            (top_recommendations, similar_recommendations, metadata)
        """
        start_time = time.time()
        metadata = {
            'user_id': user_id,
            'timestamp': datetime.now().isoformat(),
            'latency_ms': 0,
            'pipeline_version': '2.0.0',
            'ab_test_group': None
        }
        
        try:
            # Phase 0: A/B Test Routing
            model_context = self.registry.get_model_for_request(user_id)
            metadata['ab_test_group'] = 'control'  # Simplified
            
            # Phase 1: Check cold start
            if self.cold_start.is_cold_user(user_id):
                metadata['cold_start'] = True
                cold_recs = self.cold_start.get_recommendations(user_id, n=self.top_n + self.similar_n)
                top = cold_recs[:self.top_n]
                similar = cold_recs[self.top_n:self.top_n + self.similar_n]
                metadata['latency_ms'] = round((time.time() - start_time) * 1000, 2)
                return top, similar, metadata
            
            metadata['cold_start'] = False
            
            # Phase 2: Candidate Generation
            candidates = self.candidate_gen.get_candidates(user_id, k=self.candidate_k)
            if not candidates:
                logger.warning(f"No candidates for user {user_id}, falling back to popular")
                cold_recs = self.cold_start.get_recommendations(user_id, n=self.top_n + self.similar_n)
                top = cold_recs[:self.top_n]
                similar = cold_recs[self.top_n:self.top_n + self.similar_n]
                metadata['latency_ms'] = round((time.time() - start_time) * 1000, 2)
                metadata['fallback'] = 'no_candidates'
                return top, similar, metadata
            
            # Phase 3: Feature Engineering
            features_df = self._compute_features(user_id, candidates)
            
            # Phase 4: Ranking
            scores = self.ranker.score(features_df)
            
            # Phase 5: Prepare scored items for re-ranking
            scored_items = []
            for i, book_id in enumerate(candidates):
                book_feats = self.feature_store.get_book_features(book_id)
                scored_items.append({
                    'book_id': book_id,
                    'score': float(scores[i]) if i < len(scores) else 0.0,
                    'genre': book_feats.get('genre', 'unknown'),
                    'available_copies': book_feats.get('available_copies', 1),
                    'days_since_added': book_feats.get('days_since_added', 999),
                    'reason': 'ranked'
                })
            
            # Phase 6: Re-Ranking
            top, similar = self.reranker.rerank(scored_items, user_id, top_n=self.top_n, similar_n=self.similar_n)
            
            metadata['latency_ms'] = round((time.time() - start_time) * 1000, 2)
            metadata['candidates'] = len(candidates)
            metadata['fallback'] = None
            
            # Check latency budget
            if metadata['latency_ms'] > self.latency_budget_ms:
                logger.warning(f"Latency budget exceeded: {metadata['latency_ms']}ms for user {user_id}")
            
            return top, similar, metadata
            
        except Exception as e:
            logger.error(f"Pipeline error for user {user_id}: {e}")
            metadata['error'] = str(e)
            metadata['latency_ms'] = round((time.time() - start_time) * 1000, 2)
            
            # Graceful fallback
            cold_recs = self.cold_start.get_recommendations(user_id, n=self.top_n + self.similar_n)
            top = cold_recs[:self.top_n]
            similar = cold_recs[self.top_n:self.top_n + self.similar_n]
            metadata['fallback'] = 'pipeline_error'
            return top, similar, metadata
    
    def _compute_features(self, user_id: str, candidates: List[str]):
        """Compute features for ranking."""
        from scripts.feature_engineering import compute_features
        return compute_features(user_id, candidates, self.feature_store)
    
    def record_feedback(self, user_id: str, book_id: str, action: str, metadata: Dict = None):
        """Record user feedback for model improvement."""
        book_feats = self.feature_store.get_book_features(book_id)
        genre = book_feats.get('genre', 'unknown')
        
        # Update MAB
        clicked = action in ['click', 'borrow', 'wishlist']
        self.reranker.record_feedback(book_id, genre, clicked)
        
        # Log for ClickHouse
        logger.info(f"Feedback: user={user_id} book={book_id} action={action} genre={genre}")
        
        # Could also update real-time feature store here
        self.feature_store.redis.delete(f"user:{user_id}")  # Invalidate cache


# Singleton instance
_pipeline = None

def get_pipeline():
    global _pipeline
    if _pipeline is None:
        _pipeline = RecommendationPipeline()
    return _pipeline

