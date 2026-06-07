"""Knowledge Reflection Layer - links new docs to existing ones."""
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime
from collections import defaultdict

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from RAG.services.embeddings import EmbeddingGenerator

logger = logging.getLogger(__name__)


class KnowledgeReflectionEngine:
    """
    Links new documents to existing knowledge and synthesizes insights.
    Key capabilities:
    - Semantic similarity linking
    - Contradiction detection
    - Insight synthesis across documents
    - Knowledge graph building
    """

    def __init__(self, mongo_db, vector_store):
        self.db = mongo_db
        self.vs = vector_store
        self.embedder = EmbeddingGenerator()
        self.links_collection = self.db["knowledge_links"]
        self.insights_collection = self.db["knowledge_insights"]
        self.docs_collection = self.db["knowledge_documents"]
        self.similarity_threshold = 0.75

    def process_new_document(self, doc: Dict[str, Any]) -> Dict[str, Any]:
        """
        Main entry point: process a new document and link to existing knowledge.
        Returns linking report.
        """
        doc_id = doc["_id"]
        logger.info(f"Reflecting on document: {doc_id}")

        # 1. Find similar existing documents
        similar = self._find_similar_documents(doc)

        # 2. Detect contradictions
        contradictions = self._detect_contradictions(doc, similar)

        # 3. Find bridging concepts
        bridges = self._find_bridging_concepts(doc, similar)

        # 4. Synthesize cross-document insights
        insights = self._synthesize_insights(doc, similar, bridges)

        # 5. Store links
        links = self._store_links(doc_id, similar, contradictions, bridges, insights)

        # 6. Update document with reflection metadata
        self.docs_collection.update_one(
            {"_id": doc_id},
            {"$set": {
                "reflection": {
                    "linked_docs": [s["doc_id"] for s in similar],
                    "contradictions": contradictions,
                    "bridges": bridges,
                    "insights": [i["insight_id"] for i in insights],
                    "reflected_at": datetime.utcnow()
                }
            }}
        )

        return {
            "doc_id": doc_id,
            "similar_documents": len(similar),
            "contradictions_found": len(contradictions),
            "bridging_concepts": bridges,
            "insights_generated": len(insights),
            "links_created": len(links)
        }

    def _find_similar_documents(self, doc: Dict[str, Any],
                                 top_k: int = 10) -> List[Dict[str, Any]]:
        """Find semantically similar existing documents."""
        query = doc.get("title", "") + " " + doc.get("summary", "")
        results = self.vs.search(query, top_k=top_k * 2)

        # Group by document, get best chunk per doc
        doc_scores = defaultdict(list)
        for r in results:
            if r.get("doc_id") != doc.get("_id"):
                doc_scores[r["doc_id"]].append(r["similarity_score"])

        # Average score per document
        similar = []
        for did, scores in doc_scores.items():
            avg_score = sum(scores) / len(scores)
            if avg_score >= self.similarity_threshold:
                similar.append({
                    "doc_id": did,
                    "similarity_score": avg_score,
                    "matched_chunks": len(scores)
                })

        return sorted(similar, key=lambda x: x["similarity_score"], reverse=True)[:top_k]

    def _detect_contradictions(self, new_doc: Dict[str, Any],
                                similar_docs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Detect contradictions between new doc and similar docs."""
        contradictions = []
        new_text = new_doc.get("content", "")

        for sim in similar_docs:
            existing = self.docs_collection.find_one({"_id": sim["doc_id"]})
            if not existing:
                continue

            existing_text = existing.get("content", "")
            contradiction = self._check_contradiction(new_text, existing_text)

            if contradiction:
                contradictions.append({
                    "doc_id": sim["doc_id"],
                    "doc_title": existing.get("title", ""),
                    "contradiction_type": contradiction["type"],
                    "new_claim": contradiction["new_claim"],
                    "existing_claim": contradiction["existing_claim"],
                    "confidence": contradiction["confidence"],
                    "detected_at": datetime.utcnow()
                })

        return contradictions

    def _check_contradiction(self, text1: str, text2: str) -> Optional[Dict[str, Any]]:
        """Check if two texts contradict each other."""
        # Simple heuristic: look for negation patterns
        # In production, use an LLM for this
        sentences1 = [s.strip() for s in text1.split(".") if len(s.strip()) > 20]
        sentences2 = [s.strip() for s in text2.split(".") if len(s.strip()) > 20]

        for s1 in sentences1:
            s1_lower = s1.lower()
            if any(w in s1_lower for w in ["not ", "no ", "never ", "cannot"]):
                for s2 in sentences2:
                    # Check if s2 says the opposite (simplified)
                    overlap = self._word_overlap(s1, s2)
                    if overlap > 0.5 and self._opposite_sentiment(s1, s2):
                        return {
                            "type": "negation",
                            "new_claim": s1,
                            "existing_claim": s2,
                            "confidence": 0.7
                        }
        return None

    @staticmethod
    def _word_overlap(s1: str, s2: str) -> float:
        words1 = set(s1.lower().split())
        words2 = set(s2.lower().split())
        return len(words1 & words2) / len(words1 | words2) if words1 or words2 else 0

    @staticmethod
    def _opposite_sentiment(s1: str, s2: str) -> bool:
        """Check if one sentence negates the other."""
        neg_words = {"not", "no", "never", "cannot", "don't", "doesn't", "isn't", "aren't"}
        s1_has_neg = any(w in s1.lower() for w in neg_words)
        s2_has_neg = any(w in s2.lower() for w in neg_words)
        return s1_has_neg != s2_has_neg

    def _find_bridging_concepts(self, new_doc: Dict[str, Any],
                                 similar_docs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Find concepts that bridge new doc with existing docs."""
        new_keywords = set(new_doc.get("keywords", []))
        bridges = []

        for sim in similar_docs:
            existing = self.docs_collection.find_one({"_id": sim["doc_id"]})
            if not existing:
                continue

            existing_keywords = set(existing.get("keywords", []))
            shared = new_keywords & existing_keywords
            unique_new = new_keywords - existing_keywords
            unique_existing = existing_keywords - new_keywords

            bridges.append({
                "doc_id": sim["doc_id"],
                "shared_concepts": list(shared),
                "novel_concepts": list(unique_new),
                "existing_concepts": list(unique_existing),
                "bridge_strength": len(shared) / max(len(new_keywords | existing_keywords), 1)
            })

        return bridges

    def _synthesize_insights(self, new_doc: Dict[str, Any],
                             similar_docs: List[Dict[str, Any]],
                             bridges: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Synthesize new insights by combining information."""
        insights = []

        # Type 1: Cross-document validation
        if len(similar_docs) >= 2:
            insights.append({
                "insight_id": f"insight_{new_doc['_id']}_validation",
                "type": "cross_validation",
                "description": f"Document validated by {len(similar_docs)} related sources",
                "source_docs": [s["doc_id"] for s in similar_docs],
                "confidence": min(0.95, 0.5 + len(similar_docs) * 0.1),
                "created_at": datetime.utcnow()
            })

        # Type 2: Novel insight detection
        for bridge in bridges:
            if bridge["bridge_strength"] > 0.3 and bridge["novel_concepts"]:
                insights.append({
                    "insight_id": f"insight_{new_doc['_id']}_{bridge['doc_id']}",
                    "type": "novel_connection",
                    "description": f"New connections found between documents via: {', '.join(bridge['shared_concepts'][:3])}",
                    "novel_concepts": bridge["novel_concepts"],
                    "source_docs": [new_doc["_id"], bridge["doc_id"]],
                    "confidence": bridge["bridge_strength"],
                    "created_at": datetime.utcnow()
                })

        # Type 3: Knowledge gap identification
        all_existing_keywords = set()
        for sim in similar_docs:
            existing = self.docs_collection.find_one({"_id": sim["doc_id"]})
            if existing:
                all_existing_keywords.update(existing.get("keywords", []))

        gaps = all_existing_keywords - set(new_doc.get("keywords", []))
        if gaps and len(similar_docs) > 1:
            insights.append({
                "insight_id": f"insight_{new_doc['_id']}_gap",
                "type": "knowledge_gap",
                "description": f"Related topics not covered: {', '.join(list(gaps)[:5])}",
                "missing_topics": list(gaps)[:10],
                "source_docs": [s["doc_id"] for s in similar_docs],
                "confidence": 0.6,
                "created_at": datetime.utcnow()
            })

        # Store insights
        for insight in insights:
            self.insights_collection.replace_one(
                {"insight_id": insight["insight_id"]},
                insight,
                upsert=True
            )

        return insights

    def _store_links(self, doc_id: str, similar: List[Dict],
                     contradictions: List[Dict], bridges: List[Dict],
                     insights: List[Dict]) -> List[Dict[str, Any]]:
        """Store knowledge links in MongoDB."""
        links = []
        for sim in similar:
            link = {
                "source_doc": doc_id,
                "target_doc": sim["doc_id"],
                "link_type": "semantic_similarity",
                "strength": sim["similarity_score"],
                "created_at": datetime.utcnow()
            }
            self.links_collection.insert_one(link)
            links.append(link)

        for c in contradictions:
            link = {
                "source_doc": doc_id,
                "target_doc": c["doc_id"],
                "link_type": "contradiction",
                "strength": c["confidence"],
                "details": c,
                "created_at": datetime.utcnow()
            }
            self.links_collection.insert_one(link)
            links.append(link)

        return links

    def get_knowledge_graph(self, doc_id: str = None,
                            depth: int = 2) -> Dict[str, Any]:
        """Get knowledge graph for a document."""
        nodes = {}
        edges = []

        def add_node(did):
            if did not in nodes:
                doc = self.docs_collection.find_one({"_id": did})
                nodes[did] = {
                    "id": did,
                    "title": doc.get("title", "Unknown") if doc else "Unknown",
                    "type": "document"
                }

        query = {"source_doc": doc_id} if doc_id else {}
        for link in self.links_collection.find(query).limit(1000):
            add_node(link["source_doc"])
            add_node(link["target_doc"])
            edges.append({
                "source": link["source_doc"],
                "target": link["target_doc"],
                "type": link["link_type"],
                "strength": link.get("strength", 0.5)
            })

        return {"nodes": list(nodes.values()), "edges": edges}

    def get_document_insights(self, doc_id: str) -> List[Dict[str, Any]]:
        """Get all insights related to a document."""
        return list(self.insights_collection.find(
            {"source_docs": doc_id}
        ).sort("created_at", -1))
