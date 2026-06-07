"""Recommendation router with online feature boosting and fallback support."""
import random
import logging
from typing import List
from fastapi import APIRouter
from pydantic import BaseModel

from api.model_registry import get_registry
from scripts.online_features import get_feature_store
from scripts.streaming_ingestion import get_streaming_ingestion

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/recommend", tags=["recommendations"])

# Seeded fallback catalog when ClickHouse is offline
FALLBACK_BOOKS = [
    {"book_id": "bk001", "title": "The Great Gatsby", "author": "F. Scott Fitzgerald", "predicted_rating": 4.5},
    {"book_id": "bk002", "title": "1984", "author": "George Orwell", "predicted_rating": 4.4},
    {"book_id": "bk003", "title": "To Kill a Mockingbird", "author": "Harper Lee", "predicted_rating": 4.3},
    {"book_id": "bk004", "title": "Pride and Prejudice", "author": "Jane Austen", "predicted_rating": 4.2},
    {"book_id": "bk005", "title": "The Catcher in the Rye", "author": "J.D. Salinger", "predicted_rating": 4.1},
    {"book_id": "bk006", "title": "Brave New World", "author": "Aldous Huxley", "predicted_rating": 4.0},
    {"book_id": "bk007", "title": "The Lord of the Rings", "author": "J.R.R. Tolkien", "predicted_rating": 4.6},
    {"book_id": "bk008", "title": "Moby Dick", "author": "Herman Melville", "predicted_rating": 3.9},
    {"book_id": "bk009", "title": "War and Peace", "author": "Leo Tolstoy", "predicted_rating": 4.2},
    {"book_id": "bk010", "title": "The Odyssey", "author": "Homer", "predicted_rating": 4.0},
]


class BookRecommendation(BaseModel):
    book_id: str
    title: str
    author: str
    predicted_rating: float
    boost_reason: str = ""


class RecommendationResponse(BaseModel):
    user_id: str
    recommendations: List[BookRecommendation]
    model_type: str
    fallback_used: bool
    online_features_used: bool


class SectionsRecommendationResponse(BaseModel):
    user_id: str
    top_recommendations: List[BookRecommendation]
    similar_recommendations: List[BookRecommendation]
    model_type: str
    fallback_used: bool
    online_features_used: bool


class ModelInfoResponse(BaseModel):
    model_type: str
    is_ready: bool
    metrics: dict


class InteractionRequest(BaseModel):
    user_id: str
    book_id: str
    interaction_type: str  # loan, rating, search, wishlist
    rating: float = None
    metadata: dict = {}


@router.get("/model/info", response_model=ModelInfoResponse)
async def model_info():
    """Get information about the currently loaded model."""
    registry = get_registry()
    return ModelInfoResponse(
        model_type=registry.model_type,
        is_ready=registry.is_ready(),
        metrics=registry.metrics
    )


@router.get("/trending")
async def trending_books(n: int = 10, days: int = 7):
    """Get trending books based on real-time ClickHouse data."""
    try:
        store = get_feature_store()
        books = store.get_trending_books(n=n, days=days)
        return {"books": books, "source": "clickhouse_realtime"}
    except Exception as e:
        logger.error(f"Trending error: {e}")
        return {"books": FALLBACK_BOOKS[:n], "source": "fallback"}


@router.post("/interact")
async def record_interaction(request: InteractionRequest):
    """Record a user interaction for real-time model updates."""
    try:
        streaming = get_streaming_ingestion()
        
        if request.interaction_type == 'loan':
            streaming.ingest_loan(request.user_id, request.book_id, request.metadata)
        elif request.interaction_type == 'rating':
            streaming.ingest_rating(request.user_id, request.book_id, request.rating, request.metadata)
        elif request.interaction_type == 'search':
            streaming.ingest_search(request.user_id, request.metadata.get('query', ''), request.metadata)
        elif request.interaction_type == 'wishlist':
            streaming.ingest_wishlist(request.user_id, request.book_id, request.metadata)
        else:
            return {"status": "error", "message": f"Unknown interaction type: {request.interaction_type}"}
        
        return {"status": "ok", "message": f"Recorded {request.interaction_type}"}
    except Exception as e:
        logger.error(f"Interaction recording error: {e}")
        return {"status": "error", "message": str(e)}


@router.get("/{user_id}", response_model=RecommendationResponse)
async def recommend(user_id: str, n: int = 5, use_online: bool = True):
    """Get top-N book recommendations for a user with optional real-time boosting."""
    registry = get_registry()
    model = registry.model
    
    if model is None:
        logger.warning("No model loaded, using fallback books")
        return _fallback_response(user_id, n)
    
    try:
        # Get all available item IDs from the model
        if hasattr(model, 'item_ids'):
            item_ids = model.item_ids.tolist() if hasattr(model.item_ids, 'tolist') else list(model.item_ids)
        elif hasattr(model, 'all_item_ids'):
            item_ids = model.all_item_ids
        elif hasattr(model, 'item_id_set'):
            item_ids = list(model.item_id_set)
        else:
            item_ids = [b["book_id"] for b in FALLBACK_BOOKS]
        
        # Generate base recommendations
        recs = model.recommend(user_id, item_ids, n=n * 2)  # Get more for re-ranking
        
        # Apply online feature boosting if requested
        online_boosted = False
        if use_online:
            try:
                recs = _apply_online_boosting(user_id, recs)
                online_boosted = True
            except Exception as e:
                logger.warning(f"Online boosting failed: {e}")
        
        # Take top N after boosting
        recs = recs[:n]
        
        # Build response
        recommendations = []
        for item_id, score, boost_reason in recs:
            # Try to get real book info from feature store
            try:
                store = get_feature_store()
                book_features = store.get_book_features(str(item_id))
                title = f"Book {item_id}" if 'error' in book_features else f"Book {item_id}"
            except:
                title = f"Book {item_id}"
            
            recommendations.append(BookRecommendation(
                book_id=str(item_id),
                title=title,
                author="Unknown",
                predicted_rating=round(float(score), 2),
                boost_reason=boost_reason
            ))
        
        return RecommendationResponse(
            user_id=user_id,
            recommendations=recommendations,
            model_type=registry.model_type,
            fallback_used=False,
            online_features_used=online_boosted
        )
    
    except Exception as e:
        logger.error(f"Recommendation error: {e}")
        return _fallback_response(user_id, n)



@router.get("/{user_id}/sections", response_model=SectionsRecommendationResponse)
async def recommend_sections(user_id: str, top_n: int = 5, similar_n: int = 10, use_online: bool = True):
    """Get two-section recommendations: top picks + you may also like."""
    registry = get_registry()
    model = registry.model
    
    if model is None:
        logger.warning("No model loaded, using fallback books for sections")
        shuffled = FALLBACK_BOOKS.copy()
        import random
        random.shuffle(shuffled)
        top = shuffled[:top_n]
        similar = shuffled[top_n:top_n + similar_n]
        return SectionsRecommendationResponse(
            user_id=user_id,
            top_recommendations=[BookRecommendation(**b, boost_reason="fallback") for b in top],
            similar_recommendations=[BookRecommendation(**b, boost_reason="fallback") for b in similar],
            model_type="fallback",
            fallback_used=True,
            online_features_used=False
        )
    
    try:
        item_ids = list(model.item_id_set) if hasattr(model, 'item_id_set') else                    (model.all_item_ids if hasattr(model, 'all_item_ids') else                     (model.item_ids.tolist() if hasattr(model, 'item_ids') and hasattr(model.item_ids, 'tolist') else                      [b["book_id"] for b in FALLBACK_BOOKS]))
        
        recs = model.recommend(user_id, item_ids, n=top_n + similar_n)
        
        online_boosted = False
        if use_online:
            try:
                recs = _apply_online_boosting(user_id, recs)
                online_boosted = True
            except Exception as e:
                logger.warning(f"Online boosting failed: {e}")
        
        top = recs[:top_n]
        similar = recs[top_n:top_n + similar_n]
        
        def to_book_rec(item):
            item_id, score, reason = item if len(item) == 3 else (*item[:2], "none")
            return BookRecommendation(
                book_id=str(item_id),
                title=f"Book {item_id}",
                author="Unknown",
                predicted_rating=round(float(score), 2),
                boost_reason=reason
            )
        
        return SectionsRecommendationResponse(
            user_id=user_id,
            top_recommendations=[to_book_rec(r) for r in top],
            similar_recommendations=[to_book_rec(r) for r in similar],
            model_type=registry.model_type,
            fallback_used=False,
            online_features_used=online_boosted
        )
    
    except Exception as e:
        logger.error(f"Recommendation sections error: {e}")
        shuffled = FALLBACK_BOOKS.copy()
        import random
        random.shuffle(shuffled)
        top = shuffled[:top_n]
        similar = shuffled[top_n:top_n + similar_n]
        return SectionsRecommendationResponse(
            user_id=user_id,
            top_recommendations=[BookRecommendation(**b, boost_reason="fallback") for b in top],
            similar_recommendations=[BookRecommendation(**b, boost_reason="fallback") for b in similar],
            model_type="fallback",
            fallback_used=True,
            online_features_used=False
        )

def _apply_online_boosting(user_id: str, recs: list) -> list:
    """Apply real-time feature boosting to recommendations."""
    store = get_feature_store()
    
    # Get user features
    user_features = store.get_user_features(user_id)
    user_genres = user_features.get('genre_preferences', {})
    
    boosted_recs = []
    for item_id, score in recs:
        boost = 0.0
        reasons = []
        
        # Genre preference boost
        try:
            book_features = store.get_book_features(str(item_id))
            book_genre = book_features.get('genre', 'unknown')
            if book_genre in user_genres:
                genre_boost = min(0.3, user_genres[book_genre] * 0.05)
                boost += genre_boost
                reasons.append(f"genre_match:{book_genre}")
        except:
            pass
        
        # Trending boost
        try:
            book_features = store.get_book_features(str(item_id))
            trending = book_features.get('trending_score', 0)
            if trending > 10:
                boost += min(0.2, trending * 0.01)
                reasons.append("trending")
        except:
            pass
        
        # Engagement-based boost
        engagement = user_features.get('engagement_score', 0)
        if engagement > 50:
            boost += 0.1
            reasons.append("high_engagement")
        
        new_score = score + boost
        boost_reason = ", ".join(reasons) if reasons else "none"
        boosted_recs.append((item_id, new_score, boost_reason))
    
    # Re-sort by boosted score
    boosted_recs.sort(key=lambda x: x[1], reverse=True)
    return boosted_recs


def _fallback_response(user_id: str, n: int):
    """Return shuffled fallback books when model fails."""
    shuffled = FALLBACK_BOOKS.copy()
    random.shuffle(shuffled)
    selected = shuffled[:min(n, len(shuffled))]
    
    return RecommendationResponse(
        user_id=user_id,
        recommendations=[
            BookRecommendation(**book, boost_reason="fallback") for book in selected
        ],
        model_type="fallback",
        fallback_used=True,
        online_features_used=False
    )
