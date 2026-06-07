"""FAISS-based candidate generation for recommendation system."""
import os
import json
import numpy as np

FAISS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'models', 'faiss')


class FaissCandidateGenerator:
    """Retrieve top-K candidate books using FAISS ANN index."""
    
    def __init__(self):
        self.index = None
        self.book_ids = []
        self.user_embeddings = None
        self.user_ids = []
        self._load()
    
    def _load(self):
        """Load FAISS index and mappings."""
        try:
            import faiss
            index_path = os.path.join(FAISS_DIR, 'als_index.faiss')
            if os.path.exists(index_path):
                self.index = faiss.read_index(index_path)
                print(f"Loaded FAISS index: {self.index.ntotal} items")
            else:
                print("WARNING: No FAISS index found. Candidates will be empty.")
                return
            
            embeddings_path = os.path.join(FAISS_DIR, 'user_embeddings.npy')
            if os.path.exists(embeddings_path):
                self.user_embeddings = np.load(embeddings_path)
            
            book_ids_path = os.path.join(FAISS_DIR, 'book_ids.json')
            if os.path.exists(book_ids_path):
                with open(book_ids_path) as f:
                    self.book_ids = json.load(f)
            
            print(f"Candidate generator ready: {len(self.book_ids)} books")
        except ImportError:
            print("ERROR: faiss-cpu not installed")
        except Exception as e:
            print(f"ERROR loading FAISS: {e}")
    
    def get_candidates(self, user_id: str, k: int = 200) -> list:
        """Return top-k candidate book IDs for a user."""
        if self.index is None or not self.book_ids:
            return []
        
        try:
            import faiss
            
            # Find user embedding
            user_idx = self.user_ids.index(user_id) if user_id in self.user_ids else None
            if user_idx is None:
                # Cold user: return popular items (first k)
                return self.book_ids[:k]
            
            user_emb = self.user_embeddings[user_idx:user_idx+1].astype('float32')
            faiss.normalize_L2(user_emb)
            
            scores, indices = self.index.search(user_emb, k)
            return [self.book_ids[i] for i in indices[0] if i < len(self.book_ids)]
        except Exception as e:
            print(f"FAISS search error: {e}")
            return self.book_ids[:k]
