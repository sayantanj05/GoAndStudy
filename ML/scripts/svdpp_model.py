"""SVD++ collaborative filtering recommender using scikit-surprise (optional)."""
import numpy as np
import pandas as pd

# Try to import surprise, but don't fail if not available
try:
    from surprise import SVD, SVDpp, Dataset, Reader
    from surprise.model_selection import cross_validate
    SURPRISE_AVAILABLE = True
except ImportError:
    SURPRISE_AVAILABLE = False
    print("WARNING: scikit-surprise not installed. SVD/SVD++ will fall back to NMF.")


class SVDppRecommender:
    """SVD++ recommender using scikit-surprise when available.
    
    If scikit-surprise is not installed, gracefully falls back to NMF.
    SVD++ is an extension of SVD that also models implicit feedback
    (which items users interacted with, not just ratings).
    """
    
    def __init__(self, n_factors=50, n_epochs=20, lr_all=0.005, reg_all=0.02, 
                 use_svdpp=True, random_state=42):
        self.n_factors = n_factors
        self.n_epochs = n_epochs
        self.lr_all = lr_all
        self.reg_all = reg_all
        self.use_svdpp = use_svdpp
        self.random_state = random_state
        self.model = None
        self.trainset = None
        self.item_ids = None
        self.user_ids = None
        self.cv_results = None
        self._fallback_model = None
        
    def fit(self, df):
        """Train SVD++ or fall back to NMF."""
        df = df.copy()
        df.columns = ['user_id', 'item_id', 'rating']
        
        # Ensure ratings are in valid range
        df['rating'] = df['rating'].clip(1.0, 5.0)
        
        if not SURPRISE_AVAILABLE:
            print("scikit-surprise not available, falling back to NMF...")
            from .recommender_model import NMFRecommender
            self._fallback_model = NMFRecommender(n_factors=self.n_factors)
            self._fallback_model.fit(df)
            self.item_ids = self._fallback_model.item_ids
            self.user_ids = self._fallback_model.user_ids
            return self
        
        # Create surprise dataset
        reader = Reader(rating_scale=(1.0, 5.0))
        data = Dataset.load_from_df(df[['user_id', 'item_id', 'rating']], reader)
        self.trainset = data.build_full_trainset()
        
        # Store IDs for later
        self.user_ids = df['user_id'].unique()
        self.item_ids = df['item_id'].unique()
        
        # Choose model: SVD++ for implicit feedback, SVD for pure ratings
        if self.use_svdpp:
            print(f"Training SVD++ with {self.n_factors} factors, {self.n_epochs} epochs...")
            self.model = SVDpp(
                n_factors=self.n_factors,
                n_epochs=self.n_epochs,
                lr_all=self.lr_all,
                reg_all=self.reg_all,
                random_state=self.random_state,
                verbose=True
            )
        else:
            print(f"Training SVD with {self.n_factors} factors, {self.n_epochs} epochs...")
            self.model = SVD(
                n_factors=self.n_factors,
                n_epochs=self.n_epochs,
                lr_all=self.lr_all,
                reg_all=self.reg_all,
                random_state=self.random_state,
                verbose=True
            )
        
        self.model.fit(self.trainset)
        
        # Cross-validation for metrics
        print("Running 3-fold cross-validation...")
        self.cv_results = cross_validate(
            self.model, data, measures=['RMSE', 'MAE'], 
            cv=3, verbose=False, return_train_measures=True
        )
        
        rmse_mean = np.mean(self.cv_results['test_rmse'])
        mae_mean = np.mean(self.cv_results['test_mae'])
        print(f"CV RMSE: {rmse_mean:.4f}, MAE: {mae_mean:.4f}")
        
        return self
    
    def predict(self, user_id, item_id):
        """Predict rating for a user-item pair."""
        if self._fallback_model is not None:
            return self._fallback_model.predict(user_id, item_id)
        
        pred = self.model.predict(str(user_id), str(item_id))
        return type('Prediction', (), {'est': pred.est})()
    
    def recommend(self, user_id, item_ids, n=5):
        """Get top-N recommendations for a user."""
        if self._fallback_model is not None:
            return self._fallback_model.recommend(user_id, item_ids, n=n)
        
        predictions = []
        for item_id in item_ids:
            pred = self.predict(user_id, item_id)
            predictions.append((item_id, pred.est))
        
        predictions.sort(key=lambda x: x[1], reverse=True)
        return predictions[:n]
    
    def get_metrics(self):
        """Return cross-validation metrics."""
        if self._fallback_model is not None:
            return {'type': 'NMF_fallback', 'note': 'scikit-surprise not installed'}
        
        if self.cv_results is None:
            return {}
        return {
            'rmse_mean': float(np.mean(self.cv_results['test_rmse'])),
            'rmse_std': float(np.std(self.cv_results['test_rmse'])),
            'mae_mean': float(np.mean(self.cv_results['test_mae'])),
            'mae_std': float(np.std(self.cv_results['test_mae'])),
            'fit_time_mean': float(np.mean(self.cv_results['fit_time'])),
            'test_time_mean': float(np.mean(self.cv_results['test_time'])),
        }
