"""Chat API router for RAG chatbot."""
import os
import uuid
import logging
from typing import List, Optional
from datetime import datetime

from fastapi import APIRouter, HTTPException
from RAG.api.middleware import rate_limit, cache_response
from pydantic import BaseModel

from RAG.services.rag_pipeline import RAGPipeline

logger = logging.getLogger(__name__)
router = APIRouter()

# Lazy initialization
_pipeline = None


def get_pipeline():
    global _pipeline
    if _pipeline is None:
        import pymongo
        mongo_uri = os.getenv("MONGO_URI", "mongodb://localhost:27017")
        client = pymongo.MongoClient(mongo_uri)
        db = client["goandstudy"]
        _pipeline = RAGPipeline(db)
    return _pipeline


class ChatRequest(BaseModel):
    message: str
    member_id: Optional[str] = None
    conversation_id: Optional[str] = None


class ChatResponse(BaseModel):
    answer: str
    sources: List[str]
    confidence: float
    interaction_id: str
    conversation_id: str


class FeedbackRequest(BaseModel):
    interaction_id: str
    is_helpful: bool
    comment: Optional[str] = None


class DocumentRequest(BaseModel):
    title: str
    content: str
    doc_type: str = "general"
    metadata: Optional[dict] = None


@router.post("/ask", response_model=ChatResponse)
@rate_limit()
@cache_response()
async def ask_question(req: ChatRequest):
    """Ask a question to the chatbot."""
    try:
        pipeline = get_pipeline()
        conv_id = req.conversation_id or str(uuid.uuid4())
        result = pipeline.answer(
            query=req.message,
            member_id=req.member_id,
            conversation_id=conv_id
        )
        return ChatResponse(**result)
    except Exception as e:
        logger.error(f"Chat error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/feedback")
async def submit_feedback(req: FeedbackRequest):
    """Submit feedback for an interaction."""
    try:
        pipeline = get_pipeline()
        success = pipeline.submit_feedback(req.interaction_id, {
            "is_helpful": req.is_helpful,
            "comment": req.comment
        })
        return {"success": success}
    except Exception as e:
        logger.error(f"Feedback error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/documents")
async def add_document(req: DocumentRequest):
    """Add a document to the knowledge base."""
    try:
        pipeline = get_pipeline()
        result = pipeline.add_document(
            title=req.title,
            content=req.content,
            doc_type=req.doc_type,
            metadata=req.metadata
        )
        return result
    except Exception as e:
        logger.error(f"Add document error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/history/{conversation_id}")
async def get_history(conversation_id: str):
    """Get chat history for a conversation."""
    try:
        pipeline = get_pipeline()
        history = list(pipeline.interactions_collection.find(
            {"conversation_id": conversation_id},
            {"query": 1, "answer": 1, "confidence": 1, "created_at": 1}
        ).sort("created_at", -1).limit(50))
        for h in history:
            h["_id"] = str(h["_id"])
            h["created_at"] = h["created_at"].isoformat() if "created_at" in h else None
        return {"conversation_id": conversation_id, "messages": history}
    except Exception as e:
        logger.error(f"History error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stats")
async def get_stats():
    """Get chatbot statistics."""
    try:
        pipeline = get_pipeline()
        return pipeline.get_stats()
    except Exception as e:
        logger.error(f"Stats error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/insights/{doc_id}")
async def get_insights(doc_id: str):
    """Get knowledge insights for a document."""
    try:
        pipeline = get_pipeline()
        insights = pipeline.reflection.get_document_insights(doc_id)
        for i in insights:
            i["_id"] = str(i["_id"])
        return {"doc_id": doc_id, "insights": insights}
    except Exception as e:
        logger.error(f"Insights error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/knowledge-graph")
async def get_knowledge_graph(doc_id: Optional[str] = None):
    """Get knowledge graph visualization data."""
    try:
        pipeline = get_pipeline()
        return pipeline.reflection.get_knowledge_graph(doc_id)
    except Exception as e:
        logger.error(f"Graph error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
