"""Train XGBoost LambdaRank for recommendation ranking."""
import argparse
import json
import os
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import GroupShuffleSplit

from scripts.data_loader import load_interactions
from scripts.feature_engineering import compute_features
from services.feature_store import RedisFeatureStore


def generate_training_data(n_candidates_per_user: int = 200):
    """Generate labeled training data from historical interactions."""
    store = RedisFeatureStore()
    df_interactions = load_interactions()  # user_id, book_id, rating
    
    # Positive labels: borrowed books (rating >= 3.5 or from fact_loan)
    positives = df_interactions[df_interactions['rating'] >= 3.5].copy()
    positives['label'] = 1
    
    # Negative sampling: for each user, sample books they did NOT interact with
    all_books = df_interactions['book_id'].unique()
    negatives = []
    
    for user_id in positives['user_id'].unique():
        user_pos_books = set(positives[positives['user_id'] == user_id]['book_id'])
        neg_books = [b for b in all_books if b not in user_pos_books]
        sampled = pd.Series(neg_books).sample(min(n_candidates_per_user, len(neg_books)), random_state=42)
        for book_id in sampled:
            negatives.append({'user_id': user_id, 'book_id': book_id, 'label': 0})
    
    df_neg = pd.DataFrame(negatives)
    df_all = pd.concat([positives[['user_id', 'book_id', 'label']], df_neg], ignore_index=True)
    
    # Compute features for all pairs
    feature_rows = []
    for _, row in df_all.iterrows():
        feats = compute_features(row['user_id'], [row['book_id']], store)
        if not feats.empty:
            feats['label'] = row['label']
            feature_rows.append(feats)
    
    return pd.concat(feature_rows, ignore_index=True)


def train(df: pd.DataFrame):
    """Train XGBoost rank:ndcg model."""
    feature_cols = [c for c in df.columns if c not in ['user_id', 'book_id', 'label']]
    X = df[feature_cols]
    y = df['label']
    
    # Group by user for pairwise ranking
    groups = df.groupby('user_id').size().values
    
    dtrain = xgb.DMatrix(X, label=y, group=groups)
    
    params = {
        'objective': 'rank:ndcg',
        'ndcg_exp_gain': False,
        'max_depth': 6,
        'learning_rate': 0.05,
        'subsample': 0.8,
        'colsample_bytree': 0.8,
        'tree_method': 'hist',
        'eval_metric': 'ndcg@10',
    }
    
    model = xgb.train(
        params, dtrain,
        num_boost_round=500,
        evals=[(dtrain, 'train')],
        early_stopping_rounds=20
    )
    
    # Save
    output_dir = 'ML/models/xgboost_ranker'
    os.makedirs(output_dir, exist_ok=True)
    model.save_model(os.path.join(output_dir, 'ranker.json'))
    
    # Feature importance
    importance = model.get_score(importance_type='gain')
    with open(os.path.join(output_dir, 'feature_importance.json'), 'w') as f:
        json.dump(importance, f, indent=2)
    
    print(f"Saved ranker to {output_dir}, features: {len(importance)}")
    return model


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--candidates', type=int, default=200)
    args = parser.parse_args()
    
    df = generate_training_data(args.candidates)
    train(df)

