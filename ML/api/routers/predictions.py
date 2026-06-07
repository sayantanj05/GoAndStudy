"""
Prediction API endpoints for churn, overdue, and fine estimation.
Integrates Phase 4 models into the production ML service.
"""

import logging
from typing import List, Dict, Optional
from fastapi import APIRouter
from pydantic import BaseModel

from scripts.predictive_models import get_churn_predictor, get_overdue_predictor, get_fine_estimator

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/predict", tags=["predictions"])


# ============ Request/Response Models ============

class ChurnPredictionResponse(BaseModel):
    user_id: str
    user_name: str
    membership_type: str
    churn_probability: float
    risk_level: str
    engagement_score: float
    total_loans: int
    days_since_last_loan: int
    top_features: List[Dict[str, float]]

class OverduePredictionRequest(BaseModel):
    loan_id: Optional[str] = None

class OverduePredictionResponse(BaseModel):
    loan_id: str
    user_id: str
    member_name: str
    book_title: str
    status: str
    overdue_probability: float
    risk_level: str
    member_prev_overdue: int
    book_total_issues: int

class FinePredictionRequest(BaseModel):
    loan_id: Optional[str] = None
    days_overdue: int = 0

class FinePredictionResponse(BaseModel):
    loan_id: str
    user_id: str
    member_name: str
    book_title: str
    actual_fine: float
    expected_fine: float
    days_overdue: int
    member_overdue_count: int

class BatchChurnResponse(BaseModel):
    predictions: List[ChurnPredictionResponse]
    model_trained: bool
    total_members: int
    high_risk_count: int


# ============ Endpoints ============

@router.get("/churn/{user_id}", response_model=ChurnPredictionResponse)
async def predict_churn_single(user_id: str):
    """Get churn prediction for a specific member."""
    predictor = get_churn_predictor()
    results = predictor.predict(user_id=user_id)
    
    if not results:
        return ChurnPredictionResponse(
            user_id=user_id,
            user_name="Unknown",
            membership_type="",
            churn_probability=0.0,
            risk_level="UNKNOWN",
            engagement_score=0.0,
            total_loans=0,
            days_since_last_loan=0,
            top_features=[]
        )
    
    r = results[0]
    return ChurnPredictionResponse(**r)


@router.get("/churn", response_model=BatchChurnResponse)
async def predict_churn_all():
    """Get churn predictions for all members."""
    predictor = get_churn_predictor()
    results = predictor.predict()
    
    predictions = [ChurnPredictionResponse(**r) for r in results]
    high_risk = sum(1 for p in predictions if p.risk_level == "HIGH")
    
    return BatchChurnResponse(
        predictions=predictions,
        model_trained=predictor.model is not None,
        total_members=len(predictions),
        high_risk_count=high_risk
    )


@router.post("/overdue", response_model=List[OverduePredictionResponse])
async def predict_overdue(request: OverduePredictionRequest):
    """Predict overdue risk for loans."""
    predictor = get_overdue_predictor()
    results = predictor.predict(loan_id=request.loan_id)
    
    return [OverduePredictionResponse(**r) for r in results]


@router.post("/fine", response_model=List[FinePredictionResponse])
async def predict_fine(request: FinePredictionRequest):
    """Estimate fine amount for loans."""
    estimator = get_fine_estimator()
    results = estimator.predict(loan_id=request.loan_id, days_overdue=request.days_overdue)
    
    return [FinePredictionResponse(**r) for r in results]


@router.get("/model-status")
async def model_status():
    """Check status of all predictive models."""
    churn = get_churn_predictor()
    overdue = get_overdue_predictor()
    fine = get_fine_estimator()
    
    return {
        "churn_model": {
            "trained": churn.model is not None,
            "feature_count": len(churn.feature_names) if churn.model else 0,
            "features": churn.feature_names
        },
        "overdue_model": {
            "trained": overdue.model is not None,
            "feature_count": len(overdue.feature_names) if overdue.model else 0,
            "features": overdue.feature_names
        },
        "fine_model": {
            "trained": fine.model is not None,
            "feature_count": len(fine.feature_names) if fine.model else 0,
            "features": fine.feature_names
        }
    }
