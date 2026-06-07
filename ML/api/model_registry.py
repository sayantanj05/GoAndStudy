"""Model registry - loads and serves the best trained recommender model."""
import os
import json
import joblib
import logging

logger = logging.getLogger(__name__)

# Import all model classes for pickle deserialization
from scripts.recommender_model import NMFRecommender
from scripts.svdpp_model import SVDppRecommender
from scripts.hybrid_model import HybridRecommender

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'models', 'recommender')
BEST_MODEL_PATH = os.path.join(MODEL_DIR, 'best_model.joblib')
COMPAT_MODEL_PATH = os.path.join(MODEL_DIR, 'nmf_model.joblib')
METRICS_PATH = os.path.join(MODEL_DIR, 'model_metrics.json')


class ModelRegistry:
    """Registry for loading and accessing the best trained model."""
    
    def __init__(self):
        self._model = None
        self._metrics = {}
        self._model_type = "unknown"
        
    def load(self):
        """Load the best model from disk."""
        # Try best_model first
        if os.path.exists(BEST_MODEL_PATH):
            try:
                self._model = joblib.load(BEST_MODEL_PATH)
                logger.info(f"Loaded best model from {BEST_MODEL_PATH}")
            except Exception as e:
                logger.warning(f"Failed to load best_model: {e}")
                self._model = None
        
        # Fallback to nmf_model for compatibility
        if self._model is None and os.path.exists(COMPAT_MODEL_PATH):
            try:
                self._model = joblib.load(COMPAT_MODEL_PATH)
                logger.info(f"Loaded compatibility model from {COMPAT_MODEL_PATH}")
            except Exception as e:
                logger.warning(f"Failed to load nmf_model: {e}")
                self._model = None
        
        # Load metrics if available
        if os.path.exists(METRICS_PATH):
            try:
                with open(METRICS_PATH, 'r') as f:
                    self._metrics = json.load(f)
                self._model_type = self._metrics.get('best_model_type', 'unknown')
                logger.info(f"Model metrics loaded: {self._model_type}, RMSE={self._metrics.get('best_rmse', 'N/A')}")
            except Exception as e:
                logger.warning(f"Failed to load metrics: {e}")
                self._metrics = {}
        
        if self._model is None:
            logger.error("No model found! Run: python scripts/train_recommender.py")
        
        return self._model is not None
    
    @property
    def model(self):
        if self._model is None:
            self.load()
        return self._model
    
    @property
    def metrics(self):
        return self._metrics
    
    @property
    def model_type(self):
        return self._model_type
    
    def is_ready(self):
        return self._model is not None


# Global singleton
_registry = None

def get_registry():
    global _registry
    if _registry is None:
        _registry = ModelRegistry()
        _registry.load()
    return _registry

