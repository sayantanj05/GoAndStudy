"""GoAndStudy ML Service - FastAPI application."""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routers import health, recommend, admin, predictions, recommend_v2

app = FastAPI(
    title="GoAndStudy ML Service",
    description="Real-time book recommendation and prediction service with ClickHouse online features",
    version="3.0.0"
)

# CORS for frontend and Java backend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health.router, prefix="/api/v1")
app.include_router(recommend.router, prefix="/api/v1")
app.include_router(recommend_v2.router)  # Production v2 pipeline
app.include_router(admin.router, prefix="/api/v1")
app.include_router(predictions.router, prefix="/api/v1")

@app.get("/")
async def root():
    return {
        "message": "GoAndStudy ML Service",
        "version": "2.2.0",
        "phases_completed": [
            "1-Infrastructure",
            "2-BasicModel",
            "3-Frontend",
            "4-AdvancedModels",
            "5-Retraining",
            "6-AdminFeatures",
            "7-ClickHouseOnline",
            "8-PredictiveModels"
        ],
        "phases_remaining": [],
        "endpoints": {
            "health": "/api/v1/health",
            "recommend": "/api/v1/recommend/{user_id}?n=5&use_online=true",
            "trending": "/api/v1/recommend/trending?n=10",
            "interact": "POST /api/v1/recommend/interact",
            "model_info": "/api/v1/recommend/model/info",
            "admin_metrics": "/api/v1/admin/ml-metrics",
            "admin_history": "/api/v1/admin/ml-history",
            "admin_churn": "/api/v1/admin/churn-alerts",
            "admin_overdue": "/api/v1/admin/overdue-predictions",
            "predict_churn": "/api/v1/predict/churn/{user_id}",
            "predict_churn_all": "/api/v1/predict/churn",
            "predict_overdue": "POST /api/v1/predict/overdue",
            "predict_fine": "POST /api/v1/predict/fine",
            "model_status": "/api/v1/predict/model-status"
        }
    }
