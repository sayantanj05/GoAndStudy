"""Train and compare multiple recommender models, save the best one."""
import sys
import os
import json
import joblib
import pandas as pd
import numpy as np

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.recommender_model import NMFRecommender
from scripts.svdpp_model import SVDppRecommender
from scripts.hybrid_model import HybridRecommender
from scripts.data_loader import load_interactions


def generate_synthetic_data(n_users=50, n_items=100, n_interactions=500, random_state=42):
    """Generate synthetic loan/rating data for testing."""
    np.random.seed(random_state)
    
    users = [f"user_{i:03d}" for i in range(n_users)]
    items = [f"book_{i:03d}" for i in range(n_items)]
    
    data = []
    for _ in range(n_interactions):
        user = np.random.choice(users)
        item = np.random.choice(items)
        # Simulate ratings: power users rate higher, popular books rated higher
        base_rating = np.random.normal(3.5, 1.0)
        rating = np.clip(base_rating, 1.0, 5.0)
        data.append([user, item, round(rating, 1)])
    
    df = pd.DataFrame(data, columns=['user_id', 'item_id', 'rating'])
    
    # Generate book metadata for hybrid model
    genres = ['Fiction', 'Sci-Fi', 'History', 'Science', 'Philosophy', 
              'Technology', 'Biography', 'Mystery', 'Romance', 'Fantasy']
    
    book_metadata = {}
    for item in items:
        book_genres = np.random.choice(genres, size=np.random.randint(1, 4), replace=False).tolist()
        book_metadata[item] = {
            'genres': book_genres,
            'vector': np.random.randn(10).tolist(),
            'popularity': np.random.randint(1, 100)
        }
    
    # Generate user history
    user_history = {}
    for user in users:
        n_books = np.random.randint(1, 10)
        user_history[user] = np.random.choice(items, size=n_books, replace=False).tolist()
    
    return df, book_metadata, user_history


def train_and_compare():
    """Train all models and save the best one."""
    print("=" * 60)
    print("ML MODEL TRAINING PIPELINE")
    print("=" * 60)
    
    # Load or generate data
    print("\n[1/5] Loading interaction data...")
    try:
        df = load_interactions()
        print(f"Loaded {len(df)} interactions from ClickHouse")
        book_metadata = None
        user_history = None
    except Exception as e:
        print(f"ClickHouse unavailable ({e}), using synthetic data...")
        df, book_metadata, user_history = generate_synthetic_data()
        print(f"Generated {len(df)} synthetic interactions")
    
    if len(df) < 10:
        print("Insufficient data, generating synthetic dataset...")
        df, book_metadata, user_history = generate_synthetic_data()
    
    # Ensure data has correct columns
    df.columns = ['user_id', 'item_id', 'rating']
    
    # Split for evaluation
    from sklearn.model_selection import train_test_split
    train_df, test_df = train_test_split(df, test_size=0.2, random_state=42)
    
    models = {}
    metrics = {}
    
    # Model 1: NMF (baseline)
    print("\n[2/5] Training NMF model...")
    try:
        nmf = NMFRecommender(n_factors=30)
        nmf.fit(train_df)
        nmf_rmse = evaluate_model(nmf, test_df)
        models['nmf'] = nmf
        metrics['nmf'] = {'rmse': nmf_rmse, 'type': 'NMF'}
        print(f"NMF RMSE: {nmf_rmse:.4f}")
    except Exception as e:
        print(f"NMF training failed: {e}")
        metrics['nmf'] = {'rmse': float('inf'), 'type': 'NMF'}
    
    # Model 2: SVD
    print("\n[3/5] Training SVD model...")
    try:
        svd = SVDppRecommender(n_factors=50, use_svdpp=False)
        svd.fit(train_df)
        svd_rmse = evaluate_model(svd, test_df)
        models['svd'] = svd
        metrics['svd'] = {'rmse': svd_rmse, 'type': 'SVD'}
        print(f"SVD RMSE: {svd_rmse:.4f}")
    except Exception as e:
        print(f"SVD training failed: {e}")
        metrics['svd'] = {'rmse': float('inf'), 'type': 'SVD'}
    
    # Model 3: SVD++
    print("\n[4/5] Training SVD++ model...")
    try:
        svdpp = SVDppRecommender(n_factors=50, use_svdpp=True)
        svdpp.fit(train_df)
        svdpp_rmse = evaluate_model(svdpp, test_df)
        models['svdpp'] = svdpp
        metrics['svdpp'] = {'rmse': svdpp_rmse, 'type': 'SVD++'}
        # Add CV metrics if available
        cv_metrics = svdpp.get_metrics()
        if cv_metrics:
            metrics['svdpp'].update(cv_metrics)
        print(f"SVD++ RMSE: {svdpp_rmse:.4f}")
    except Exception as e:
        print(f"SVD++ training failed: {e}")
        metrics['svdpp'] = {'rmse': float('inf'), 'type': 'SVD++'}
    
    # Model 4: Hybrid
    print("\n[5/5] Training Hybrid model...")
    try:
        hybrid = HybridRecommender(
            cf_weight=0.5, content_weight=0.3, popularity_weight=0.2,
            n_factors=50, use_svdpp=True
        )
        hybrid.fit(train_df, book_metadata=book_metadata, user_history=user_history)
        hybrid_rmse = evaluate_model(hybrid, test_df)
        models['hybrid'] = hybrid
        metrics['hybrid'] = {'rmse': hybrid_rmse, 'type': 'Hybrid'}
        print(f"Hybrid RMSE: {hybrid_rmse:.4f}")
    except Exception as e:
        print(f"Hybrid training failed: {e}")
        metrics['hybrid'] = {'rmse': float('inf'), 'type': 'Hybrid'}
    
    # Select best model
    print("\n" + "=" * 60)
    print("MODEL COMPARISON")
    print("=" * 60)
    
    best_model_name = None
    best_rmse = float('inf')
    
    for name, m in metrics.items():
        rmse = m.get('rmse', float('inf'))
        model_type = m.get('type', name.upper())
        status = "✅ BEST" if rmse < best_rmse else ""
        if rmse < best_rmse:
            best_rmse = rmse
            best_model_name = name
        print(f"  {model_type:12s} RMSE: {rmse:.4f} {status}")
    
    if best_model_name and best_model_name in models:
        best_model = models[best_model_name]
        print(f"\n🏆 Best model: {metrics[best_model_name]['type']} (RMSE: {best_rmse:.4f})")
        
        # Save best model
        model_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'models', 'recommender')
        os.makedirs(model_dir, exist_ok=True)
        
        model_path = os.path.join(model_dir, 'best_model.joblib')
        joblib.dump(best_model, model_path)
        print(f"Saved best model to: {model_path}")
        
        # Save metrics
        metrics_path = os.path.join(model_dir, 'model_metrics.json')
        with open(metrics_path, 'w') as f:
            json.dump({
                'best_model': best_model_name,
                'best_model_type': metrics[best_model_name]['type'],
                'best_rmse': best_rmse,
                'all_metrics': metrics,
                'training_samples': len(df),
                'test_samples': len(test_df)
            }, f, indent=2, default=str)
        print(f"Saved metrics to: {metrics_path}")
        
        # Also save as nmf_model.joblib for backward compatibility
        compat_path = os.path.join(model_dir, 'nmf_model.joblib')
        joblib.dump(best_model, compat_path)
        print(f"Saved compatibility model to: {compat_path}")
        
        return best_model, metrics
    else:
        print("\n❌ No model trained successfully!")
        return None, metrics


def evaluate_model(model, test_df):
    """Calculate RMSE on test set."""
    predictions = []
    actuals = []
    
    for _, row in test_df.iterrows():
        try:
            pred = model.predict(row['user_id'], row['item_id'])
            pred_rating = pred.est if hasattr(pred, 'est') else 3.0
            predictions.append(pred_rating)
            actuals.append(row['rating'])
        except Exception:
            predictions.append(3.0)
            actuals.append(row['rating'])
    
    predictions = np.array(predictions)
    actuals = np.array(actuals)
    rmse = np.sqrt(np.mean((predictions - actuals) ** 2))
    return rmse


if __name__ == "__main__":
    train_and_compare()

