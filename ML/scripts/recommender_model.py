"""NMF-based collaborative filtering recommender model."""
import numpy as np
from sklearn.decomposition import NMF
from sklearn.metrics import mean_squared_error


class NMFRecommender:
    """NMF-based collaborative filtering recommender."""
    
    def __init__(self, n_factors=50, random_state=42):
        self.n_factors = n_factors
        self.random_state = random_state
        self.nmf = None
        self.user_ids = None
        self.item_ids = None
        self.user_id_map = {}
        self.item_id_map = {}
        self.user_factors = None
        self.item_factors = None
        self.global_mean = 0.0
        
    def fit(self, df):
        """Train the NMF model on interaction data."""
        df = df.copy()
        df.columns = ['user_id', 'item_id', 'rating']
        
        # Create mappings
        self.user_ids = df['user_id'].unique()
        self.item_ids = df['item_id'].unique()
        self.user_id_map = {uid: i for i, uid in enumerate(self.user_ids)}
        self.item_id_map = {iid: i for i, iid in enumerate(self.item_ids)}
        
        # Build user-item matrix
        n_users = len(self.user_ids)
        n_items = len(self.item_ids)
        self.global_mean = df['rating'].mean()
        
        # Adjust n_factors to not exceed matrix dimensions
        actual_factors = min(self.n_factors, min(n_users, n_items))
        if actual_factors < self.n_factors:
            print(f"Adjusted n_factors from {self.n_factors} to {actual_factors} (matrix: {n_users}x{n_items})")
        
        print(f"Building {n_users}x{n_items} user-item matrix...")
        matrix = np.full((n_users, n_items), self.global_mean)
        
        for _, row in df.iterrows():
            u_idx = self.user_id_map[row['user_id']]
            i_idx = self.item_id_map[row['item_id']]
            matrix[u_idx, i_idx] = row['rating']
        
        # Normalize by subtracting global mean
        matrix_norm = matrix - self.global_mean
        matrix_norm = np.clip(matrix_norm, 0, None)  # NMF requires non-negative
        
        print(f"Training NMF with {actual_factors} factors...")
        self.nmf = NMF(
            n_components=actual_factors,
            random_state=self.random_state,
            max_iter=500,
            init='random'
        )
        self.user_factors = self.nmf.fit_transform(matrix_norm)
        self.item_factors = self.nmf.components_.T
        
        # Calculate reconstruction error
        reconstructed = self.user_factors @ self.item_factors.T + self.global_mean
        rmse = np.sqrt(mean_squared_error(matrix, reconstructed))
        print(f"Training RMSE: {rmse:.4f}")
        
        return self
    
    def predict(self, user_id, item_id):
        """Predict rating for a user-item pair."""
        if user_id not in self.user_id_map or item_id not in self.item_id_map:
            # Cold start: return global mean
            return type('Prediction', (), {'est': self.global_mean})()
        
        u_idx = self.user_id_map[user_id]
        i_idx = self.item_id_map[item_id]
        
        pred = self.user_factors[u_idx].dot(self.item_factors[i_idx]) + self.global_mean
        pred = np.clip(pred, 1.0, 5.0)
        
        return type('Prediction', (), {'est': float(pred)})()
    
    def recommend(self, user_id, item_ids, n=5):
        """Get top-N recommendations for a user."""
        predictions = []
        for item_id in item_ids:
            pred = self.predict(user_id, item_id)
            predictions.append((item_id, pred.est))
        
        predictions.sort(key=lambda x: x[1], reverse=True)
        return predictions[:n]
