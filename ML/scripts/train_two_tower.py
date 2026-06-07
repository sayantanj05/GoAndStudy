"""Train Two-Tower Neural Network for candidate generation."""
import os
import json
import numpy as np
import pandas as pd
import tensorflow as tf
import tensorflow_recommenders as tfrs

from scripts.data_loader import load_interactions, load_book_metadata


class TwoTowerModel(tfrs.Model):
    def __init__(self, embedding_dim: int = 64, vocab_size: int = 10000):
        super().__init__()
        self.embedding_dim = embedding_dim
        
        # User tower
        self.user_embedding = tf.keras.Sequential([
            tf.keras.layers.StringLookup(vocabulary=[], mask_token=None),
            tf.keras.layers.Embedding(vocab_size + 1, embedding_dim)
        ])
        
        # Item tower
        self.item_embedding = tf.keras.Sequential([
            tf.keras.layers.StringLookup(vocabulary=[], mask_token=None),
            tf.keras.layers.Embedding(vocab_size + 1, embedding_dim)
        ])
        
        # Metrics
        self.task = tfrs.tasks.Retrieval(
            metrics=tfrs.metrics.FactorizedTopK(
                candidates=tf.data.Dataset.from_tensor_slices(np.array([])).batch(128)
            )
        )
    
    def compute_loss(self, features, training=False):
        user_embeddings = self.user_embedding(features['user_id'])
        item_embeddings = self.item_embedding(features['book_id'])
        
        # Compute the loss using the retrieval task
        return self.task(user_embeddings, item_embeddings)


def train(embedding_dim: int = 64, epochs: int = 10):
    df = load_interactions()
    
    # Build vocabularies
    user_ids = df['user_id'].unique().tolist()
    book_ids = df['book_id'].unique().tolist()
    
    dataset = tf.data.Dataset.from_tensor_slices({
        'user_id': df['user_id'].values,
        'book_id': df['book_id'].values
    }).batch(2048)
    
    model = TwoTowerModel(embedding_dim=embedding_dim, vocab_size=max(len(user_ids), len(book_ids)))
    
    # Set vocabularies
    model.user_embedding.layers[0].set_vocabulary(user_ids)
    model.item_embedding.layers[0].set_vocabulary(book_ids)
    
    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.001))
    model.fit(dataset, epochs=epochs)
    
    # Save user tower
    output_dir = 'ML/models/two_tower'
    os.makedirs(output_dir, exist_ok=True)
    model.user_embedding.save(os.path.join(output_dir, 'user_tower'))
    
    # Save item tower and extract embeddings for FAISS
    model.item_embedding.save(os.path.join(output_dir, 'item_tower'))
    
    # Pre-compute all item embeddings
    item_dataset = tf.data.Dataset.from_tensor_slices(np.array(book_ids)).batch(128)
    item_embeddings = []
    for batch in item_dataset:
        emb = model.item_embedding(batch)
        item_embeddings.extend(emb.numpy())
    item_embeddings = np.array(item_embeddings, dtype='float32')
    
    np.save(os.path.join(output_dir, 'item_embeddings.npy'), item_embeddings)
    with open(os.path.join(output_dir, 'book_ids.json'), 'w') as f:
        json.dump(book_ids, f)
    
    print(f"Two-tower trained. Embeddings shape: {item_embeddings.shape}")
    return model, item_embeddings, book_ids


if __name__ == '__main__':
    train(embedding_dim=64, epochs=10)

