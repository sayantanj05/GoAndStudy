"""Minimal NLP utilities for Self-Learning RAG."""
import re
import string
from typing import List, Dict, Any, Optional
from collections import Counter

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer


class SemanticChunker:
    def __init__(self, chunk_size=512, chunk_overlap=128, min_length=50):
        self.cs = chunk_size
        self.co = chunk_overlap
        self.ml = min_length

    def chunk_text(self, text: str) -> List[Dict[str, Any]]:
        if not text or not text.strip():
            return []
        text = re.sub(r'\s+', ' ', text).strip()
        paragraphs = [p.strip() for p in text.split('\n\n') if p.strip()]
        chunks, cur, start, idx = [], '', 0, 0
        for p in paragraphs:
            if len(cur) + len(p) > self.cs and len(cur) >= self.ml:
                chunks.append({'text': cur.strip(), 'start_char': start,
                              'end_char': start + len(cur), 'chunk_index': idx})
                idx += 1
                ov = self._overlap(cur)
                cur = (ov + ' ' + p) if ov else p
                start = start + len(cur) - len(p) - len(ov) - 1 if ov else start
            else:
                if cur:
                    cur += ' '
                else:
                    start = text.find(p)
                cur += p
        if len(cur) >= self.ml:
            chunks.append({'text': cur.strip(), 'start_char': start,
                          'end_char': start + len(cur), 'chunk_index': idx})
        return chunks

    def _overlap(self, text: str) -> str:
        sents = [s.strip() for s in re.split(r'(?<=[.!?])\s+', text) if s.strip()]
        ov = ''
        for s in reversed(sents):
            if len(ov) + len(s) <= self.co:
                ov = s + ' ' + ov
            else:
                break
        return ov.strip()


class EntityExtractor:
    PATTERNS = {
        'BOOK_TITLE': r'\b[A-Z][A-Za-z\s:\-]+(?:Book|Novel|Series|Volume|Edition|Guide)\b',
        'AUTHOR': r'\b[A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*\b',
        'DATE': r'\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2}(?:,\s+\d{4})?)\b',
        'ISBN': r'\b(?:ISBN[-\s:]*)?(?:97[89][-\s]?)?\d{1,5}[-\s]?\d{1,7}[-\s]?\d{1,7}[-\s]?[\dX]\b',
        'FINE': r'\$\d+(?:\.\d{2})?',
    }

    def extract(self, text: str) -> List[Dict[str, Any]]:
        entities = []
        for label, pattern in self.PATTERNS.items():
            for m in re.finditer(pattern, text):
                entities.append({'text': m.group(), 'label': label,
                                'start': m.start(), 'end': m.end(), 'source': 'rule'})
        entities = sorted(entities, key=lambda e: (e['start'], -(e['end'] - e['start'])))
        filtered, last_end = [], -1
        for e in entities:
            if e['start'] >= last_end:
                filtered.append(e)
                last_end = e['end']
        return filtered


class KeywordExtractor:
    def __init__(self, top_n=10):
        self.top_n = top_n
        self.sw = {'the', 'a', 'an', 'is', 'are', 'was', 'were', 'be', 'been',
                   'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would',
                   'to', 'of', 'in', 'for', 'on', 'with', 'at', 'by', 'from',
                   'as', 'and', 'but', 'or', 'it', 'this', 'that', 'which'}

    def extract(self, text: str, ngram=(1, 2)) -> List[str]:
        if not text or len(text.strip()) < 10:
            return []
        k1 = self._tfidf(text, ngram)
        k2 = self._freq(text)
        seen = set()
        result = []
        for k in k1 + k2:
            n = k.lower().strip()
            if n not in seen and len(n) > 2:
                seen.add(n)
                result.append(k)
        return result[:self.top_n]

    def _tfidf(self, text, ngram):
        sents = [s.strip() for s in re.split(r'[.!?]+', text) if len(s.strip()) > 10]
        if len(sents) < 2:
            return []
        try:
            v = TfidfVectorizer(max_df=0.85, min_df=1, ngram_range=ngram,
                               stop_words='english', max_features=100)
            m = v.fit_transform(sents)
            fn = v.get_feature_names_out()
            scores = np.mean(m.toarray(), axis=0)
            idx = np.argsort(scores)[::-1][:self.top_n]
            return [fn[i] for i in idx if scores[i] > 0]
        except Exception:
            return []

    def _freq(self, text):
        words = [w for w in re.findall(r'\b[A-Za-z][a-zA-Z]*\b', text.lower())
                if w not in self.sw and len(w) > 3]
        wc = Counter(words)
        caps = re.findall(r'\b[A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*\b', text)
        for c in caps:
            if len(c) > 3:
                wc[c.lower()] += 3
        return [w for w, _ in wc.most_common(self.top_n * 2)]


class NLPPipeline:
    def __init__(self, cfg=None):
        self.cfg = cfg or {}
        self.chunker = SemanticChunker(
            self.cfg.get('chunk_size', 512),
            self.cfg.get('chunk_overlap', 128),
            self.cfg.get('min_length', 50)
        )
        self.ee = EntityExtractor()
        self.ke = KeywordExtractor(self.cfg.get('keywords', 10))

    def process_doc(self, text: str) -> Dict[str, Any]:
        chunks = self.chunker.chunk_text(text)
        result = {
            'word_count': len(text.split()),
            'keywords': self.ke.extract(text),
            'entities': self.ee.extract(text),
            'chunks': chunks
        }
        for c in chunks:
            c['keywords'] = self.ke.extract(c['text'])
            c['entities'] = self.ee.extract(c['text'])
        return result

    def process_query(self, query: str) -> Dict[str, Any]:
        return {
            'text': query,
            'keywords': self.ke.extract(query),
            'entities': self.ee.extract(query)
        }
