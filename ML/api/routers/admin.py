"""Admin dashboard endpoints for ML monitoring and predictions."""
import logging
from typing import List, Dict
from fastapi import APIRouter
from pydantic import BaseModel

from api.model_registry import get_registry

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/admin", tags=["admin"])

class ModelMetricsResponse(BaseModel):
    model_type: str
    is_ready: bool
    rmse: float
    training_samples: int
    last_trained: str

class ChurnAlert(BaseModel):
    user_id: str
    user_name: str
    churn_probability: float
    risk_level: str
    last_active_days: int

class OverduePrediction(BaseModel):
    loan_id: str
    member_name: str
    book_title: str
    due_date: str
    overdue_probability: float
    estimated_fine: float

@router.get("/ml-metrics", response_model=ModelMetricsResponse)
async def ml_metrics():
    """Get current ML model performance metrics."""
    registry = get_registry()
    metrics = registry.metrics
    
    return ModelMetricsResponse(
        model_type=registry.model_type,
        is_ready=registry.is_ready(),
        rmse=metrics.get('best_rmse', 0.0),
        training_samples=metrics.get('training_samples', 0),
        last_trained=metrics.get('timestamp', 'unknown')
    )

@router.get("/ml-history", response_model=List[Dict])
async def ml_history():
    """Get model performance history over time."""
    import os
    import json
    from datetime import datetime
    
    perf_file = os.path.join(os.path.dirname(__file__), '..', '..', 'models', 'recommender', 'performance_history.json')
    if os.path.exists(perf_file):
        with open(perf_file, 'r') as f:
            return json.load(f)
    return []

@router.get("/churn-alerts", response_model=List[ChurnAlert])
async def churn_alerts(threshold: float = 0.7):
    """Get members at high risk of churning."""
    try:
        from scripts.data_loader import load_member_analytics
        import pandas as pd
        
        df = load_member_analytics()
        if df is None or len(df) == 0:
            return []
        
        # Calculate churn risk (simplified heuristic)
        df['churn_probability'] = (
            (df['days_since_last_loan'].fillna(30) / 30).clip(0, 1) * 0.4 +
            (1 - df['total_loans'].fillna(0).clip(0, 10) / 10) * 0.3 +
            (df['total_overdue'].fillna(0).clip(0, 5) / 5) * 0.3
        )
        
        high_risk = df[df['churn_probability'] >= threshold].sort_values('churn_probability', ascending=False)
        
        alerts = []
        for _, row in high_risk.head(20).iterrows():
            prob = float(row['churn_probability'])
            alerts.append(ChurnAlert(
                user_id=str(row.get('user_id', '')),
                user_name=str(row.get('name', 'Unknown')),
                churn_probability=round(prob, 2),
                risk_level="HIGH" if prob > 0.8 else "MEDIUM",
                last_active_days=int(row.get('days_since_last_loan', 30))
            ))
        
        return alerts
    except Exception as e:
        logger.error(f"Churn prediction error: {e}")
        return []

@router.get("/overdue-predictions", response_model=List[OverduePrediction])
async def overdue_predictions():
    """Predict which active loans are likely to become overdue."""
    try:
        from scripts.data_loader import load_active_loans
        import pandas as pd
        
        df = load_active_loans()
        if df is None or len(df) == 0:
            return []
        
        # Simple heuristic: days held / expected duration
        df['overdue_probability'] = (
            (df['days_held'].fillna(0) / df['max_days'].fillna(14)).clip(0, 1) * 0.6 +
            (df['previous_overdues'].fillna(0).clip(0, 5) / 5) * 0.4
        )
        
        at_risk = df[df['overdue_probability'] > 0.5].sort_values('overdue_probability', ascending=False)
        
        predictions = []
        for _, row in at_risk.head(20).iterrows():
            predictions.append(OverduePrediction(
                loan_id=str(row.get('loan_id', '')),
                member_name=str(row.get('member_name', 'Unknown')),
                book_title=str(row.get('book_title', 'Unknown')),
                due_date=str(row.get('due_date', '')),
                overdue_probability=round(float(row['overdue_probability']), 2),
                estimated_fine=round(float(row.get('fine_rate', 0.5)) * max(0, row['days_held'] - row['max_days']), 2)
            ))
        
        return predictions
    except Exception as e:
        logger.error(f"Overdue prediction error: {e}")
        return []
