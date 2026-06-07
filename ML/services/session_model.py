"""Real-time session intent computation."""
import tensorflow as tf
import numpy as np
import os

class SessionIntentModel:
    def __init__(self):
        model_path = 'ML/models/session_transformer'
        if os.path.exists(model_path):
            self.model = tf.keras.models.load_model(model_path)
        else:
            self.model = None
        self.book_to_idx = {}  # loaded from vocab
        self.action_to_idx = {'loan': 0, 'rating': 1, 'search': 2, 'wishlist': 3}
    
    def compute_intent_vector(self, recent_interactions: list) -> np.ndarray:
        """recent_interactions: [{book_id, action_type, timestamp}]"""
        if self.model is None:
            return None
        # Pad/truncate to MAX_SEQ_LEN
        seq = recent_interactions[-20:]
        book_ids = [self.book_to_idx.get(i['book_id'], 0) for i in seq]
        actions = [self.action_to_idx.get(i['action_type'], 0) for i in seq]
        
        # Pad
        while len(book_ids) < 20:
            book_ids.insert(0, 0)
            actions.insert(0, 0)
        
        pred = self.model.predict({
            'book_ids': np.array([book_ids]),
            'action_types': np.array([actions])
        })
        return pred[0]  # Intent vector over all books

