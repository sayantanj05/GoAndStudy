"""Production recommendation router v2 with full pipeline integration."""
import logging
from typing import List
from fastapi import APIRouter
from pydantic import BaseModel

from services.recommendation_pipeline import get_pipeline
from services.monitoring import get_metrics
from scripts.streaming_ingestion import get_streaming_ingestion

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/recommend/v2", tags=["recommendations-v2"])


class BookRecommendation(BaseModel):
    book_id: str
    title: str = ""
    author: str = ""
    score: float = 0.0
    reason: str = ""
    genre: str = ""


class RecommendationResponse(BaseModel):
    user_id: str
    top_recommendations: List[BookRecommendation]
    you_may_also_like: List[BookRecommendation]
    latency_ms: float
    model_version: str = "2.0.0"
    cold_start: bool = False
    fallback: str = None


class FeedbackRequest(BaseModel):
    user_id: str
    book_id: str
    action: str  # click, borrow, ignore, dismiss
    position: int = 0


@router.get("/{user_id}", response_model=RecommendationResponse)
async def recommend_v2(user_id: str, top_n: int = 5, similar_n: int = 10):
    """Production recommendation endpoint with full FAANG-level pipeline."""
    pipeline = get_pipeline()
    metrics = get_metrics()
    
    top, similar, meta = pipeline.recommend(user_id)
    
    # Record metrics
    metrics.record_request(meta.get('pipeline_version', '2.0.0'), meta.get('cold_start', False), meta.get('fallback'))
    metrics.record_latency(meta.get('latency_ms', 0) / 1000.0)
    metrics.set_candidate_pool(meta.get('candidates', 0))
    
    def to_book_rec(item):
        return BookRecommendation(
            book_id=item['book_id'],
            score=round(item.get('score', 0.0), 3),
            reason=item.get('reason', ''),
            genre=item.get('genre', '')
        )
    
    return RecommendationResponse(
        user_id=user_id,
        top_recommendations=[to_book_rec(r) for r in top],
        you_may_also_like=[to_book_rec(r) for r in similar],
        latency_ms=meta.get('latency_ms', 0),
        cold_start=meta.get('cold_start', False),
        fallback=meta.get('fallback')
    )


@router.post("/feedback")
async def record_feedback(request: FeedbackRequest):
    """Record user feedback for real-time model improvement."""
    pipeline = get_pipeline()
    metrics = get_metrics()
    
    pipeline.record_feedback(request.user_id, request.book_id, request.action)
    metrics.record_feedback(request.action, 'unknown')
    
    # Also stream to ClickHouse
    streaming = get_streaming_ingestion()
    streaming.ingest_loan(request.user_id, request.book_id, {
        'action': request.action,
        'position': request.position,
        'source': 'recommendation'
    })
    
    return {"status": "ok", "message": f"Recorded {request.action} feedback"}


@router.get("/metrics")
async def prometheus_metrics():
    """Expose Prometheus metrics."""
    from fastapi.responses import Response
    metrics = get_metrics()
    return Response(content=metrics.get_metrics(), media_type="text/plain")

