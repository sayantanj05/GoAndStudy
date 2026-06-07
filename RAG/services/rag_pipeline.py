"""Main RAG Pipeline with self-learning capabilities."""
import os
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime

from RAG.services.nlp_utils import NLPPipeline
from RAG.services.vector_store import VectorStore
from RAG.services.knowledge_reflection import KnowledgeReflectionEngine
from RAG.models.knowledge_base_impl import KnowledgeBase

logger = logging.getLogger(__name__)


class RAGPipeline:
    """
    Self-Learning RAG Pipeline with knowledge reflection.
    Handles: query processing, retrieval, generation, learning, feedback.
    """

    def __init__(self, mongo_db, config: Dict[str, Any] = None):
        self.cfg = config or {}
        self.db = mongo_db
        self.kb = KnowledgeBase(mongo_db)
        self.vs = VectorStore(mongo_db)
        self.reflection = KnowledgeReflectionEngine(mongo_db, self.vs)
        self.nlp = NLPPipeline(self.cfg.get('nlp', {}))
        self.feedback_collection = self.db["chatbot_feedback"]
        self.interactions_collection = self.db["chatbot_interactions"]

    def answer(self, query: str, member_id: str = None,
               conversation_id: str = None) -> Dict[str, Any]:
        """
        Main entry point: answer a member query.
        """
        start_time = datetime.utcnow()

        # 1. Process query with NLP
        query_meta = self.nlp.process_query(query)

        # 2. Retrieve relevant chunks
        chunks = self._retrieve(query, query_meta)

        # 3. Generate answer
        answer = self._generate(query, chunks)

        # 4. Log interaction
        interaction = self._log_interaction(
            query, query_meta, chunks, answer,
            member_id, conversation_id, start_time
        )

        # 5. Trigger self-learning if needed
        self._self_learn(interaction)

        return {
            "answer": answer["text"],
            "sources": [c["doc_title"] for c in chunks],
            "confidence": answer.get("confidence", 0.0),
            "interaction_id": str(interaction["_id"]),
            "conversation_id": conversation_id
        }

    def _retrieve(self, query: str,
                  query_meta: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Retrieve relevant chunks using hybrid search."""
        keywords = query_meta.get("keywords", [])

        # Hybrid search: vector + keyword
        results = self.vs.hybrid_search(query, keywords, top_k=5)

        # Rerank by recency and feedback score
        for r in results:
            age_days = (datetime.utcnow() - r.get("created_at", datetime.utcnow())).days
            r["recency_boost"] = max(0, 1.0 - age_days / 365)
            r["final_score"] = (
                r.get("similarity_score", 0) * 0.5 +
                r.get("feedback_score", 0) * 0.2 +
                r["recency_boost"] * 0.1
            )

        results.sort(key=lambda x: x["final_score"], reverse=True)
        return results[:5]

    def _generate(self, query: str,
                  chunks: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Generate answer from retrieved chunks."""
        if not chunks:
            return {
                "text": "I don't have specific information about that. Please contact library staff for assistance.",
                "confidence": 0.0
            }

        context = "\n\n".join([
            f"[Source: {c.get('doc_title', 'Unknown')}]\n{c['text']}"
            for c in chunks
        ])

        # Simple template-based generation (replace with LLM in production)
        answer = self._template_generate(query, context, chunks)

        return {
            "text": answer,
            "confidence": min(0.95, 0.5 + len(chunks) * 0.1)
        }

    def _template_generate(self, query: str, context: str,
                           chunks: List[Dict[str, Any]]) -> str:
        """Template-based answer generation."""
        q_lower = query.lower()

        if any(w in q_lower for w in ["hours", "open", "close", "time"]):
            return self._extract_hours(context)

        if any(w in q_lower for w in ["location", "where", "address", "find"]):
            return self._extract_location(context)

        if any(w in q_lower for w in ["borrow", "loan", "check out", "how many"]):
            return self._extract_loan_policy(context)

        if any(w in q_lower for w in ["fine", "fee", "cost", "charge", "overdue"]):
            return self._extract_fines(context)

        if any(w in q_lower for w in ["book", "recommend", "suggest", "similar"]):
            return self._extract_recommendations(context)

        # Default: summarize context
        return self._summarize_context(query, context)

    def _extract_hours(self, context: str) -> str:
        import re
        hours = re.findall(r'(\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm)?\s*-\s*\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm)?)', context)
        if hours:
            return f"The library is open during these hours: {', '.join(hours[:3])}. Please check the website for holiday schedules."
        return "Library hours vary by day. Please check the website or call for current hours."

    def _extract_location(self, context: str) -> str:
        import re
        locations = re.findall(r'\b(?:Room|Floor|Building|Section)\s+[A-Z0-9]+\b', context)
        if locations:
            return f"You can find it at: {', '.join(locations[:3])}."
        return "Please check the library map or ask staff for directions."

    def _extract_loan_policy(self, context: str) -> str:
        import re
        numbers = re.findall(r'\b\d+\s*(?:books?|items?|days?|weeks?)\b', context)
        if numbers:
            return f"Based on our policies: {', '.join(numbers[:3])}. Please check your account for your specific limits."
        return "Loan policies vary by membership type. Please check your account or contact staff."

    def _extract_fines(self, context: str) -> str:
        import re
        amounts = re.findall(r'\$\d+(?:\.\d{2})?', context)
        if amounts:
            return f"Fines are typically {amounts[0]} per day for overdue items. Please return books on time to avoid charges."
        return "Overdue fines apply. Please check your account or contact staff for details."

    def _extract_recommendations(self, context: str) -> str:
        lines = [l.strip() for l in context.split("\n") if l.strip() and len(l.strip()) > 20]
        if lines:
            return f"Here are some recommendations based on your query:\n\n" + "\n".join(lines[:5])
        return "I'd recommend browsing our catalog or asking staff for personalized recommendations."

    def _summarize_context(self, query: str, context: str) -> str:
        lines = [l.strip() for l in context.split("\n") if l.strip() and len(l.strip()) > 20]
        if lines:
            summary = " ".join(lines[:3])
            return f"Based on our knowledge base: {summary[:500]}..."
        return "I found some relevant information but couldn't generate a specific answer. Please rephrase your question."

    def _log_interaction(self, query: str, query_meta: Dict[str, Any],
                         chunks: List[Dict[str, Any]], answer: Dict[str, Any],
                         member_id: str, conversation_id: str,
                         start_time: datetime) -> Dict[str, Any]:
        """Log interaction to MongoDB."""
        duration = (datetime.utcnow() - start_time).total_seconds()
        interaction = {
            "query": query,
            "query_keywords": query_meta.get("keywords", []),
            "query_entities": query_meta.get("entities", []),
            "retrieved_chunks": [c["_id"] for c in chunks],
            "answer": answer["text"],
            "confidence": answer["confidence"],
            "member_id": member_id,
            "conversation_id": conversation_id,
            "duration_seconds": duration,
            "feedback": None,
            "created_at": datetime.utcnow()
        }
        result = self.interactions_collection.insert_one(interaction)
        interaction["_id"] = result.inserted_id
        return interaction

    def add_document(self, title: str, content: str,
                     doc_type: str = "general",
                     metadata: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Add a document to the knowledge base with reflection.
        """
        # 1. Add to knowledge base
        doc = self.kb.add_document(title, content, doc_type, metadata)

        # 2. Run NLP pipeline
        nlp_result = self.nlp.process_doc(content)

        # 3. Add chunks to vector store
        chunks = nlp_result.get("chunks", [])
        for chunk in chunks:
            chunk["doc_id"] = doc["_id"]
            chunk["title"] = title
        if chunks:
            self.vs.add_documents(chunks)

        # 4. Run knowledge reflection
        doc["keywords"] = nlp_result.get("keywords", [])
        doc["entities"] = nlp_result.get("entities", [])
        reflection_report = self.reflection.process_new_document(doc)

        return {
            "doc_id": doc["_id"],
            "chunks_added": len(chunks),
            "reflection": reflection_report
        }

    def submit_feedback(self, interaction_id: str, feedback: Dict[str, Any]) -> bool:
        """Submit feedback for an interaction."""
        result = self.interactions_collection.update_one(
            {"_id": interaction_id},
            {"$set": {"feedback": feedback, "feedback_at": datetime.utcnow()}}
        )
        if result.modified_count > 0 and feedback.get("is_helpful"):
            self._promote_chunks(interaction_id)
        return result.modified_count > 0

    def _promote_chunks(self, interaction_id: str):
        """Promote chunks that led to positive feedback."""
        interaction = self.interactions_collection.find_one({"_id": interaction_id})
        if not interaction:
            return
        chunk_ids = interaction.get("retrieved_chunks", [])
        if chunk_ids:
            self.db["knowledge_chunks"].update_many(
                {"_id": {"$in": chunk_ids}},
                {"$inc": {"feedback_score": 1}}
            )

    def _self_learn(self, interaction: Dict[str, Any]):
        """Trigger self-learning based on interaction patterns."""
        # Check if this query type is underrepresented
        keywords = interaction.get("query_keywords", [])
        if not keywords:
            return

        # Find knowledge gaps
        recent_queries = list(self.interactions_collection.find(
            {"created_at": {"$gte": datetime.utcnow().replace(hour=0, minute=0)}}
        ).limit(100))

        query_types = {}
        for q in recent_queries:
            for kw in q.get("query_keywords", []):
                query_types[kw] = query_types.get(kw, 0) + 1

        # If query is rare, flag for knowledge gap
        total_queries = len(recent_queries)
        for kw in keywords:
            freq = query_types.get(kw, 0)
            if total_queries > 20 and freq / total_queries < 0.05:
                self.db["knowledge_gaps"].insert_one({
                    "keyword": kw,
                    "query": interaction["query"],
                    "frequency": freq,
                    "flagged_at": datetime.utcnow()
                })

    def get_stats(self) -> Dict[str, Any]:
        """Get pipeline statistics."""
        return {
            "total_documents": self.kb.documents_collection.count_documents({}),
            "total_chunks": self.vs.get_stats()["total_vectors"],
            "total_interactions": self.interactions_collection.count_documents({}),
            "total_feedback": self.interactions_collection.count_documents({"feedback": {"$ne": None}}),
            "knowledge_links": self.db["knowledge_links"].count_documents({}),
            "insights": self.db["knowledge_insights"].count_documents({})
        }
