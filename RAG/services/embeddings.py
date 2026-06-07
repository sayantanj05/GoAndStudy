"""Embedding generator using sentence-transformers with fallback."""
import os
import logging
from typing import List
import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingGenerator:
    """Generate embeddings with local model + API fallback."""

    def __init__(self, model_name: str = None, api_key: str = None):
        self.model_name = model_name or os.getenv(
            'EMBEDDING_MODEL', 'all-MiniLM-L6-v2'
        )
        self.api_key = api_key or os.getenv('OPENAI_API_KEY')
        self._model = None
        self.dimension = 384  # Default for MiniLM

    def _load_local(self):
        if self._model is None:
            try:
                from sentence_transformers import SentenceTransformer
                self._model = SentenceTransformer(self.model_name)
                self.dimension = self._model.get_sentence_embedding_dimension()
                logger.info(f"Loaded embedding model: {self.model_name}")
            except ImportError:
                logger.warning("sentence-transformers not installed")
                raise

    def embed(self, text: str) -> np.ndarray:
        """Embed single text."""
        if not text or not text.strip():
            return np.zeros(self.dimension)
        try:
            self._load_local()
            return self._model.encode(text, convert_to_numpy=True)
        except Exception as e:
            logger.warning(f"Local embed failed: {e}")
            return self._embed_api(text)

    def embed_batch(self, texts: List[str]) -> np.ndarray:
        """Embed multiple texts."""
        texts = [t if t else "" for t in texts]
        try:
            self._load_local()
            return self._model.encode(texts, convert_to_numpy=True, batch_size=32)
        except Exception as e:
            logger.warning(f"Local batch embed failed: {e}")
            return np.array([self._embed_api(t) for t in texts])

    def _embed_api(self, text: str) -> np.ndarray:
        """Fallback to OpenAI API."""
        if not self.api_key:
            logger.error("No API key for embedding fallback")
            return np.zeros(self.dimension)
        try:
            import openai
            openai.api_key = self.api_key
            resp = openai.Embedding.create(
                input=text[:8000],
                model="text-embedding-ada-002"
            )
            return np.array(resp['data'][0]['embedding'])
        except Exception as e:
            logger.error(f"API embed failed: {e}")
            return np.zeros(self.dimension)
