"""Health check router."""
from fastapi import APIRouter

router = APIRouter()

@router.get("/health")
def health_check():
    return {"status": "ok", "service": "ml-recommender", "version": "1.0.0"}
