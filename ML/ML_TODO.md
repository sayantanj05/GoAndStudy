# ML Service Implementation Roadmap

## Phase 1: Infrastructure ✅ COMPLETE
- [x] FastAPI service on port 8001
- [x] Model registry with joblib serialization
- [x] Health check endpoint
- [x] CORS for frontend/backend integration
- [x] Fallback book catalog when ClickHouse offline

## Phase 2: Basic Recommendation Model ✅ COMPLETE
- [x] NMF collaborative filtering (scikit-learn)
- [x] Data loader from ClickHouse DW
- [x] Training script with train/test split
- [x] Model persistence to `models/recommender/nmf_model.joblib`

## Phase 3: Frontend Integration ✅ COMPLETE
- [x] Java `MlRecommendationService` with RestTemplate
- [x] `MlRecommendationResponse` DTO
- [x] `MemberController` `/api/members/{id}/recommendations` endpoint
- [x] Frontend `member.api.js` with `getRecommendations()`
- [x] `MemberHomePage.jsx` with "Recommended For You" + "Similar Books" sections
- [x] Graceful fallback to popular books when ML service unavailable

## Phase 4: Advanced ML Models ✅ COMPLETE
- [x] SVD++ model (scikit-surprise, graceful fallback to NMF)
- [x] Hybrid model (SVD++ + content-based + popularity)
- [x] Model comparison: NMF vs SVD vs SVD++ vs Hybrid
- [x] Auto-select best model by RMSE
- [x] Save best model as `best_model.joblib` with metrics JSON
- [x] Model info endpoint `/api/v1/recommend/model/info`

## Phase 5: Retraining Pipeline ✅ COMPLETE
- [x] Airflow DAG `ml_retrain_dag.py` for daily retraining at 2 AM
- [x] Data freshness check (skip if < 10 new interactions)
- [x] Versioned model artifacts with timestamps
- [x] Performance regression detection (alert if RMSE degrades > 10%)
- [x] Performance history tracking in JSON

## Phase 6: Admin Dashboard ✅ COMPLETE
- [x] `/api/v1/admin/ml-metrics` - Current model performance
- [x] `/api/v1/admin/ml-history` - Historical performance chart data
- [x] `/api/v1/admin/churn-alerts` - High-risk members (≥70% churn probability)
- [x] `/api/v1/admin/overdue-predictions` - Loans likely to become overdue

## Phase 7: ClickHouse Online Features ✅ COMPLETE
- [x] Real-time feature computation from ClickHouse (`scripts/online_features.py`)
- [x] User features: recent loans, ratings, searches, genre preferences, engagement score
- [x] Book features: recent loans, ratings, wishlist count, trending score
- [x] Trending books endpoint with real-time ClickHouse queries
- [x] Similar users discovery based on borrowing patterns
- [x] In-memory feature cache with 60-second TTL
- [x] Streaming interaction ingestion (`scripts/streaming_ingestion.py`)
- [x] Buffered writes to ClickHouse with batching (100 events / 60s)
- [x] Local JSONL backup for durability
- [x] Online recommendation boosting: genre matching, trending, engagement
- [x] Interaction recording endpoint: `POST /api/v1/recommend/interact`

## Phase 8: Feature Engineering ⏳ PENDING
- [ ] User temporal features (time-of-day patterns, seasonality)
- [ ] Book embedding enrichment (TF-IDF, BERT)
- [ ] Cross-features (user genre preferences × book attributes)
- [ ] Cold-start handling for new users/books

---

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   React Frontend │────▶│  Java Spring Boot │────▶│  ML FastAPI     │
│                 │     │   Backend (8080)  │     │  Service (8001) │
│ MemberHomePage  │◄────│  RestTemplate     │◄────│  /recommend/{id}│
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               │                           │
                               ▼                           ▼
                        ┌──────────────┐          ┌──────────────┐
                        │   MongoDB    │          │  ClickHouse  │
                        │  (Primary)   │          │     DW       │
                        └──────────────┘          └──────────────┘
```

## Running the ML Service

```bash
cd ML
python -m uvicorn api.main:app --host 0.0.0.0 --port 8001
```

## Training Models

```bash
cd ML
python scripts/train_recommender.py
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Service info |
| `/api/v1/health` | GET | Health check |
| `/api/v1/recommend/{user_id}` | GET | Get recommendations (use `?use_online=true` for real-time boosting) |
| `/api/v1/recommend/trending` | GET | Trending books from ClickHouse |
| `/api/v1/recommend/interact` | POST | Record user interaction |
| `/api/v1/recommend/model/info` | GET | Model metrics & type |
| `/api/v1/admin/ml-metrics` | GET | Current ML performance |
| `/api/v1/admin/ml-history` | GET | Performance history |
| `/api/v1/admin/churn-alerts` | GET | Churn risk members |
| `/api/v1/admin/overdue-predictions` | GET | Overdue predictions |

## Models

| Model | Type | Status |
|-------|------|--------|
| NMF | Collaborative Filtering | ✅ Active |
| SVD | Collaborative Filtering | ✅ Available |
| SVD++ | Collaborative + Implicit | ✅ Available (falls back to NMF) |
| Hybrid | SVD++ + Content + Popularity | ✅ Available |

## Online Features

| Feature | Source | Cache TTL |
|---------|--------|-----------|
| User engagement score | ClickHouse | 60s |
| Genre preferences | ClickHouse | 60s |
| Book trending score | ClickHouse | 60s |
| Trending books | ClickHouse | 60s |
| Similar users | ClickHouse | 60s |
