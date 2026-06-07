"""Build FAISS ANN index from ALS embeddings for fast candidate retrieval."""
import os
import json
import numpy as np
import pandas as pd
from sklearn.decomposition import NMF

from scripts.data_loader import load_interactions


def train_als_embeddings(n_factors: int = 64):
    """Train ALS-like matrix factorization and extract user/item embeddings."""
    df = load_interactions()
    
    # Build mappings
    user_ids = df['user_id'].unique()
    book_ids = df['book_id'].unique()
    user_map = {u: i for i, u in enumerate(user_ids)}
    book_map = {b: i for i, b in enumerate(book_ids)}
    
    # Build sparse matrix
    n_users = len(user_ids)
    n_items = len(book_ids)
    matrix = np.zeros((n_users, n_items), dtype=np.float32)
    
    for _, row in df.iterrows():
        u_idx = user_map.get(row['user_id'])
        i_idx = book_map.get(row['book_id'])
        if u_idx is not None and i_idx is not None:
            matrix[u_idx, i_idx] = row['rating']
    
    # NMF factorization (ALS approximation)
    print(f"Training NMF with {n_factors} factors on {n_users}x{n_items} matrix...")
    nmf = NMF(n_components=n_factors, random_state=42, max_iter=500, init='random')
    user_factors = nmf.fit_transform(matrix)
    item_factors = nmf.components_.T
    
    return user_factors, item_factors, user_ids, book_ids


def build_faiss_index(item_factors: np.ndarray, book_ids: list):
    """Build FAISS Inner Product index from item embeddings."""
    try:
        import faiss
    except ImportError:
        print("ERROR: faiss-cpu not installed. Install with: pip install faiss-cpu")
        return None
    
    # Normalize for cosine similarity via inner product
    faiss.normalize_L2(item_factors)
    
    dim = item_factors.shape[1]
    index = faiss.IndexFlatIP(dim)
    index.add(item_factors)
    
    # Save index and metadata
    output_dir = 'ML/models/faiss'
    os.makedirs(output_dir, exist_ok=True)
    
    faiss.write_index(index, os.path.join(output_dir, 'als_index.faiss'))
    np.save(os.path.join(output_dir, 'item_embeddings.npy'), item_factors)
    
    with open(os.path.join(output_dir, 'book_ids.json'), 'w') as f:
        json.dump(book_ids, f)
    
    print(f"FAISS index saved to {output_dir} — {len(book_ids)} items, dim={dim}")
    return index


if __name__ == '__main__':
    user_factors, item_factors, user_ids, book_ids = train_als_embeddings(n_factors=64)
    build_faiss_index(item_factors, book_ids)
