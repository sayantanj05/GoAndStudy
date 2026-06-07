"""Vector store using FAISS + MongoDB for hybrid retrieval."""
import os
import pickle
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime

import numpy as np
import faiss

from RAG.services.embeddings import EmbeddingGenerator

logger = logging.getLogger(__name__)


class VectorStore:
    """Hybrid vector store: FAISS for ANN + MongoDB for metadata."""

    def __init__(self, mongo_db, collection_name="knowledge_chunks",
                 index_path="RAG/data/faiss_index.bin",
                 metadata_path="RAG/data/faiss_metadata.pkl"):
        self.db = mongo_db
        self.collection = self.db[collection_name]
        self.index_path = index_path
        self.metadata_path = metadata_path
        self.embedder = EmbeddingGenerator()
        self.index = None
        self.chunk_metadata = {}
        self.dimension = self.embedder.dimension
        self._load_or_create_index()

    def _load_or_create_index(self):
        os.makedirs(os.path.dirname(self.index_path), exist_ok=True)
        if os.path.exists(self.index_path) and os.path.exists(self.metadata_path):
            self.index = faiss.read_index(self.index_path)
            with open(self.metadata_path, 'rb') as f:
                self.chunk_metadata = pickle.load(f)
            logger.info(f"Loaded FAISS index: {self.index.ntotal} vectors")
        else:
            self.index = faiss.IndexFlatIP(self.dimension)
            logger.info("Created new FAISS index")

    def add_documents(self, documents: List[Dict[str, Any]]) -> int:
        """Add documents to vector store. Returns count added."""
        if not documents:
            return 0
        texts = [d['text'] for d in documents]
        embeddings = self.embedder.embed_batch(texts)
        embeddings = embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)
        start_idx = self.index.ntotal
        self.index.add(embeddings.astype('float32'))
        for i, doc in enumerate(documents):
            chunk_id = f"chunk_{start_idx + i}"
            meta = {
                '_id': chunk_id,
                'text': doc['text'],
                'doc_id': doc.get('doc_id', 'unknown'),
                'doc_title': doc.get('title', ''),
                'chunk_index': doc.get('chunk_index', 0),
                'start_char': doc.get('start_char', 0),
                'end_char': doc.get('end_char', 0),
                'keywords': doc.get('keywords', []),
                'entities': doc.get('entities', []),
                'topics': doc.get('topics', []),
                'created_at': datetime.utcnow(),
                'access_count': 0,
                'feedback_score': 0.0
            }
            self.chunk_metadata[start_idx + i] = meta
            self.collection.replace_one({'_id': chunk_id}, meta, upsert=True)
        self._persist()
        logger.info(f"Added {len(documents)} chunks to vector store")
        return len(documents)

    def search(self, query: str, top_k: int = 5,
               filters: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """Search for relevant chunks."""
        q_emb = self.embedder.embed(query)
        q_emb = q_emb / np.linalg.norm(q_emb)
        scores, indices = self.index.search(
            q_emb.reshape(1, -1).astype('float32'), top_k * 3
        )
        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx < 0 or idx not in self.chunk_metadata:
                continue
            meta = self.chunk_metadata[idx].copy()
            meta['similarity_score'] = float(score)
            if filters and not self._matches_filters(meta, filters):
                continue
            results.append(meta)
            if len(results) >= top_k:
                break
        self._update_access_counts([r['_id'] for r in results])
        return results

    def _matches_filters(self, meta: Dict, filters: Dict) -> bool:
        for key, value in filters.items():
            if key in meta and meta[key] != value:
                return False
        return True

    def _update_access_counts(self, chunk_ids: List[str]):
        if chunk_ids:
            self.collection.update_many(
                {'_id': {'$in': chunk_ids}},
                {'$inc': {'access_count': 1}}
            )

    def get_stats(self) -> Dict[str, Any]:
        return {
            'total_vectors': self.index.ntotal,
            'dimension': self.dimension,
            'total_chunks': len(self.chunk_metadata)
        }

    def _persist(self):
        faiss.write_index(self.index, self.index_path)
        with open(self.metadata_path, 'wb') as f:
            pickle.dump(self.chunk_metadata, f)

    def hybrid_search(self, query: str, query_keywords: List[str],
                      top_k: int = 5) -> List[Dict[str, Any]]:
        """Combine vector + keyword search with RRF."""
        vector_results = self.search(query, top_k=top_k * 2)
        keyword_results = list(self.collection.find(
            {'keywords': {'$in': query_keywords}},
            {'text': 1, 'doc_id': 1, 'doc_title': 1}
        ).limit(top_k * 2))
        return self._reciprocal_rank_fusion(vector_results, keyword_results, top_k)

    @staticmethod
    def _reciprocal_rank_fusion(v_results, k_results, top_k, k=60):
        scores = {}
        for rank, r in enumerate(v_results):
            cid = r['_id']
            scores[cid] = scores.get(cid, 0) + 1.0 / (k + rank + 1)
            scores[cid + '_meta'] = r
        for rank, r in enumerate(k_results):
            cid = r['_id']
            scores[cid] = scores.get(cid, 0) + 1.0 / (k + rank + 1)
            if cid + '_meta' not in scores:
                scores[cid + '_meta'] = r
        ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)
        return [scores[cid + '_meta'] for cid, _ in ranked[:top_k]
                if cid + '_meta' in scores]
