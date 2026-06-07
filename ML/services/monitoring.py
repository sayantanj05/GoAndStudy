"""Prometheus monitoring for the recommendation system."""
import time
import logging
from typing import Dict

try:
    from prometheus_client import Counter, Histogram, Gauge, Info, generate_latest, CONTENT_TYPE_LATEST
    PROMETHEUS_AVAILABLE = True
except ImportError:
    PROMETHEUS_AVAILABLE = False

logger = logging.getLogger(__name__)


class RecommendationMetrics:
    """Production metrics for recommendation system."""
    
    def __init__(self):
        if not PROMETHEUS_AVAILABLE:
            logger.warning("prometheus_client not installed. Metrics will be no-ops.")
            return
        
        # Request counters
        self.recommend_requests_total = Counter(
            'recommend_requests_total',
            'Total recommendation requests',
            ['model_version', 'cold_start', 'fallback']
        )
        
        self.recommend_feedback_total = Counter(
            'recommend_feedback_total',
            'User feedback on recommendations',
            ['action', 'genre']
        )
        
        # Latency histogram
        self.recommend_latency_seconds = Histogram(
            'recommend_latency_seconds',
            'Recommendation latency',
            buckets=[0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0]
        )
        
        # Business metrics
        self.recommend_conversion_rate = Gauge(
            'recommend_conversion_rate',
            'Borrow conversion rate from recommendations',
            ['model_version']
        )
        
        self.model_ndcg = Gauge(
            'model_ndcg_at_10',
            'Offline NDCG@10 of current model',
            ['model_version']
        )
        
        # System health
        self.candidate_pool_size = Gauge(
            'candidate_pool_size',
            'Number of candidates generated'
        )
        
        self.feature_store_cache_hit = Counter(
            'feature_store_cache_hit_total',
            'Feature store cache hits',
            ['feature_type']
        )
        
        self.info = Info('recommendation_system', 'System metadata')
        self.info.info({'version': '2.0.0', 'pipeline': 'two_tower_xgboost'})
    
    def record_request(self, model_version: str, cold_start: bool, fallback: str = None):
        if PROMETHEUS_AVAILABLE:
            self.recommend_requests_total.labels(
                model_version=model_version,
                cold_start=str(cold_start),
                fallback=str(fallback or 'none')
            ).inc()
    
    def record_latency(self, latency_seconds: float):
        if PROMETHEUS_AVAILABLE:
            self.recommend_latency_seconds.observe(latency_seconds)
    
    def record_feedback(self, action: str, genre: str):
        if PROMETHEUS_AVAILABLE:
            self.recommend_feedback_total.labels(action=action, genre=genre).inc()
    
    def set_ndcg(self, model_version: str, ndcg: float):
        if PROMETHEUS_AVAILABLE:
            self.model_ndcg.labels(model_version=model_version).set(ndcg)
    
    def set_conversion_rate(self, model_version: str, rate: float):
        if PROMETHEUS_AVAILABLE:
            self.recommend_conversion_rate.labels(model_version=model_version).set(rate)
    
    def set_candidate_pool(self, size: int):
        if PROMETHEUS_AVAILABLE:
            self.candidate_pool_size.set(size)
    
    def get_metrics(self):
        if PROMETHEUS_AVAILABLE:
            return generate_latest()
        return b"# Prometheus not available\n"


# Singleton
_metrics = None

def get_metrics():
    global _metrics
    if _metrics is None:
        _metrics = RecommendationMetrics()
    return _metrics

