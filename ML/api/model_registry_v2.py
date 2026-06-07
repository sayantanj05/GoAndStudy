"""Advanced model registry with versioning, A/B testing, and shadow deployment."""
import os
import json
import time
import logging
from typing import Dict, Optional, List
import numpy as np

logger = logging.getLogger(__name__)

MODEL_BASE_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'models')
REGISTRY_STATE_PATH = os.path.join(MODEL_BASE_DIR, 'registry_state.json')


class ModelVersion:
    """Represents a single model version."""
    def __init__(self, name: str, version: str, path: str, metrics: Dict, 
                 model_type: str, created_at: float = None):
        self.name = name
        self.version = version
        self.path = path
        self.metrics = metrics
        self.model_type = model_type
        self.created_at = created_at or time.time()
        self.is_active = False
        self.traffic_percentage = 0.0
    
    def to_dict(self):
        return {
            'name': self.name,
            'version': self.version,
            'path': self.path,
            'metrics': self.metrics,
            'model_type': self.model_type,
            'created_at': self.created_at,
            'is_active': self.is_active,
            'traffic_percentage': self.traffic_percentage
        }
    
    @classmethod
    def from_dict(cls, data: Dict):
        v = cls(data['name'], data['version'], data['path'], 
                data['metrics'], data['model_type'], data['created_at'])
        v.is_active = data.get('is_active', False)
        v.traffic_percentage = data.get('traffic_percentage', 0.0)
        return v


class ModelRegistryV2:
    """Production model registry with A/B testing and canary deployment."""
    
    def __init__(self):
        self.versions: Dict[str, List[ModelVersion]] = {}
        self.current_production = None
        self.experiments = {}
        self._load_state()
    
    def _load_state(self):
        """Load registry state from disk."""
        if os.path.exists(REGISTRY_STATE_PATH):
            try:
                with open(REGISTRY_STATE_PATH, 'r') as f:
                    state = json.load(f)
                for model_name, versions_data in state.get('versions', {}).items():
                    self.versions[model_name] = [ModelVersion.from_dict(v) for v in versions_data]
                self.current_production = state.get('current_production')
                self.experiments = state.get('experiments', {})
            except Exception as e:
                logger.error(f"Failed to load registry state: {e}")
    
    def _save_state(self):
        """Persist registry state."""
        state = {
            'versions': {k: [v.to_dict() for v in vals] for k, vals in self.versions.items()},
            'current_production': self.current_production,
            'experiments': self.experiments,
            'saved_at': time.time()
        }
        os.makedirs(os.path.dirname(REGISTRY_STATE_PATH), exist_ok=True)
        with open(REGISTRY_STATE_PATH, 'w') as f:
            json.dump(state, f, indent=2)
    
    def register(self, name: str, version: str, path: str, metrics: Dict, model_type: str):
        """Register a new model version."""
        if name not in self.versions:
            self.versions[name] = []
        
        model_version = ModelVersion(name, version, path, metrics, model_type)
        self.versions[name].append(model_version)
        self._save_state()
        logger.info(f"Registered {name} version {version}")
        return model_version
    
    def deploy(self, name: str, version: str, traffic_percentage: float = 100.0):
        """Deploy a model version to production (canary or full)."""
        if name not in self.versions:
            raise ValueError(f"Model {name} not found")
        
        for v in self.versions[name]:
            if v.version == version:
                v.is_active = True
                v.traffic_percentage = traffic_percentage
                if traffic_percentage == 100.0:
                    self.current_production = f"{name}:{version}"
                    # Deactivate other versions
                    for other in self.versions[name]:
                        if other.version != version:
                            other.is_active = False
                            other.traffic_percentage = 0.0
            elif traffic_percentage == 100.0:
                v.is_active = False
        
        self._save_state()
        logger.info(f"Deployed {name}:{version} with {traffic_percentage}% traffic")
    
    def start_experiment(self, experiment_id: str, model_a: str, version_a: str, 
                         model_b: str, version_b: str, split: float = 0.5):
        """Start A/B test between two model versions."""
        self.experiments[experiment_id] = {
            'model_a': model_a,
            'version_a': version_a,
            'model_b': model_b,
            'version_b': version_b,
            'split': split,
            'started_at': time.time(),
            'metrics': {'a': [], 'b': []}
        }
        self._save_state()
        logger.info(f"Started experiment {experiment_id}: {model_a}:{version_a} vs {model_b}:{version_b}")
    
    def get_model_for_request(self, user_id: str, experiment_id: str = None):
        """Route request to appropriate model version."""
        if experiment_id and experiment_id in self.experiments:
            exp = self.experiments[experiment_id]
            # Deterministic routing based on user_id hash
            user_hash = hash(user_id) % 100 / 100.0
            if user_hash < exp['split']:
                return self._load_model(exp['model_a'], exp['version_a'])
            else:
                return self._load_model(exp['model_b'], exp['version_b'])
        
        # Production routing
        if self.current_production:
            name, version = self.current_production.split(':')
            return self._load_model(name, version)
        
        return None
    
    def _load_model(self, name: str, version: str):
        """Load model from disk."""
        for v in self.versions.get(name, []):
            if v.version == version:
                # Dynamic model loading based on type
                if v.model_type == 'xgboost':
                    import xgboost as xgb
                    model = xgb.Booster()
                    model.load_model(v.path)
                    return model
                elif v.model_type == 'tensorflow':
                    import tensorflow as tf
                    return tf.keras.models.load_model(v.path)
                elif v.model_type == 'sklearn':
                    import joblib
                    return joblib.load(v.path)
        return None
    
    def list_versions(self, name: str = None):
        """List all registered versions."""
        if name:
            return [v.to_dict() for v in self.versions.get(name, [])]
        return {k: [v.to_dict() for v in vals] for k, vals in self.versions.items()}
    
    def get_model_status(self):
        """Get current production and experiment status."""
        return {
            'current_production': self.current_production,
            'experiments': self.experiments,
            'total_versions': sum(len(v) for v in self.versions.values())
        }


# Global singleton
_registry_v2 = None

def get_registry_v2():
    global _registry_v2
    if _registry_v2 is None:
        _registry_v2 = ModelRegistryV2()
    return _registry_v2

