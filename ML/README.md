# GoAndStudy ML Service

Real-time book recommendation and prediction service for the GoAndStudy Library Management System.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Java Backend   │────▶│  ML FastAPI      │────▶│  ClickHouse DW  │
│  (Spring Boot)  │◀────│  (Port 8001)     │◀────│  (library_dw)   │
└─────────────────┘     └──────────────────┘     └─────────────────┘
```

## Phase 1: SVD++ Collaborative Filtering

**Algorithm**: SVD++ (Singular Value Decomposition Plus Plus)  
**Library**: scikit-surprise  
**Data Source**: `fact_loan` + `fact_ratings` from ClickHouse

## Quick Start

### 1. Install Dependencies
```bash
cd ML
python -m venv ml_env
ml_env\Scripts\activate  # Windows
pip install -r requirements.txt
```

### 2. Train Model
```bash
cd scripts
python train_recommender.py
```

### 3. Start API
```bash
cd ..
uvicorn api.main:app --reload --host 0.0.0.0 --port 8001
```

### 4. Test
```bash
curl http://localhost:8001/api/v1/health
curl http://localhost:8001/api/v1/recommend/USER_ID?n=5
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/health` | GET | Service health check |
| `/api/v1/recommend/{user_id}` | GET | Get top-N book recommendations |

## Folder Structure

```
ML/
├── api/                  # FastAPI application
│   ├── main.py          # App entry point
│   ├── model_registry.py # Model loader
│   └── routers/
│       ├── health.py    # Health endpoint
│       └── recommend.py # Recommendation endpoint
├── scripts/             # Training scripts
│   ├── data_loader.py   # ClickHouse queries
│   └── train_recommender.py # SVD++ training
├── models/              # Saved models
│   └── recommender/
├── config/
│   └── config.yaml      # Configuration
└── requirements.txt     # Dependencies
```

## Future Phases

| Phase | Algorithm | Use Case |
|-------|-----------|----------|
| 2 | Hybrid (SVD++ + Content + Rules) | Better accuracy |
| 3 | Decision Tree | User segmentation |
| 4 | Neural CF (TensorFlow) | Deep learning |
| 5 | DeepFM | Feature-aware recommendations |
