"""XGBoost LambdaRank wrapper for real-time scoring."""
import os
import xgboost as xgb
import pandas as pd
import numpy as np

RANKER_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'models', 'xgboost_ranker', 'ranker.json')

class XGBoostRanker:
    def __init__(self):
        self.model = None
        self._load()
    
    def _load(self):
        if os.path.exists(RANKER_PATH):
            self.model = xgb.Booster()
            self.model.load_model(RANKER_PATH)
            print(f"Ranker loaded from {RANKER_PATH}")
        else:
            print("WARNING: No ranker model found. Scores will be zero.")
    
    def score(self, features_df: pd.DataFrame) -> np.ndarray:
        """Score candidates. Returns array of relevance scores."""
        if self.model is None:
            return np.zeros(len(features_df))
        dmatrix = xgb.DMatrix(features_df)
        return self.model.predict(dmatrix)

