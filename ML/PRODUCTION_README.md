# GoAndStudy Production Recommendation System v3.0

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PRODUCTION RECOMMENDATION SYSTEM                   │
├─────────────────────────────────────────────────────────────────────────┤
│  Java Backend → FastAPI ML Service → Redis/ClickHouse Feature Store      │
│                                                                           │
│  Request Flow:                                                            │
│  1. GET /recommend/v2/{user_id}                                          │
│     ├── Cold Start Check (Phase 5)                                        │
│     ├── Candidate Generation: FAISS + Two-Tower (Phase 4)                │
│     ├── Feature Engineering: 47 features from Redis (Phase 1)            │
│     ├── Ranking: XGBoost LambdaRank (Phase 3)                            │
│     ├── Re-Ranking: Diversity + Freshness + MAB (Phase 5)                │
│     └── Response: top + similar sections                                  │
│                                                                           │
│  Batch Flow (Airflow):                                                    │
│  1. MongoDB → ClickHouse ETL (daily)                                     │
│  2. Train Two-Tower → Build FAISS Index                                  │
│  3. Train XGBoost Ranker                                                 │
│  4. Refresh Feature Store                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
ML/
├── api/
│   ├── main.py                    # FastAPI app (v3.0)
│   ├── model_registry_v2.py       # A/B testing + versioning
│   └── routers/
│       ├── recommend_v2.py        # Production pipeline endpoint
│       └── ...
├── services/
│   ├── recommendation_pipeline.py # Unified pipeline
│   ├── feature_store.py           # Redis + ClickHouse hybrid
│   ├── candidate_generator.py     # FAISS + Two-Tower
│   ├── ranker.py                  # XGBoost LambdaRank
│   ├── reranker.py                # Diversity + MAB
│   ├── cold_start_handler.py      # New user strategy
│   ├── session_model.py           # Transformer intent
│   ├── mab_explorer.py            # Thompson Sampling
│   └── monitoring.py              # Prometheus metrics
├── scripts/
│   ├── train_recommender.py       # Baseline NMF/SVD
│   ├── train_xgboost_ranker.py    # Phase 3 ranking
│   ├── train_two_tower.py         # Phase 4 embeddings
│   ├── train_session_transformer.py # Phase 5 sequences
│   ├── build_faiss_index.py       # ANN index builder
│   ├── feature_engineering.py     # 47-feature computation
│   └── streaming_ingestion.py     # Real-time events
├── config/
│   └── config.yaml                # All pipeline parameters
└── requirements.txt               # Full dependency list
```

## Phase Summary

| Phase | Component | Status | Target Metric |
|-------|-----------|--------|---------------|
| 1 | Feature Store (Redis) | ✅ Implemented | < 5ms feature lookup |
| 2 | Candidate Generation (FAISS) | ✅ Implemented | < 10ms retrieval |
| 3 | XGBoost LambdaRank | ✅ Implemented | NDCG@10 > 0.30 |
| 4 | Two-Tower Neural Network | ✅ Implemented | NDCG@10 > 0.35 |
| 5 | Session Transformer + MAB | ✅ Implemented | >20% top-5 accuracy |

## API Endpoints

### Production v2 (Recommended)
```bash
# Get recommendations
curl http://localhost:8000/recommend/v2/user_123

# Record feedback
curl -X POST http://localhost:8000/recommend/v2/feedback \
  -H "Content-Type: application/json" \
  -d '{"user_id":"user_123","book_id":"book_456","action":"borrow"}'

# Prometheus metrics
curl http://localhost:8000/recommend/v2/metrics
```

## Training Pipeline

```bash
# Phase 1: Feature store is automatic (Redis + ClickHouse)

# Phase 2: Build FAISS index
python scripts/build_faiss_index.py

# Phase 3: Train XGBoost ranker
python scripts/train_xgboost_ranker.py --candidates 200

# Phase 4: Train Two-Tower + rebuild FAISS
python scripts/train_two_tower.py
python scripts/build_faiss_index.py --use-two-tower

# Phase 5: Train session transformer
python scripts/train_session_transformer.py
```

## Monitoring

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| P95 Latency | < 100ms | > 150ms |
| Conversion Rate | > 5% | < 3% |
| NDCG@10 | > 0.35 | < 0.30 |
| Feature Store Cache Hit | > 80% | < 70% |
| Candidate Pool Size | > 200 | < 100 |

## A/B Testing

```python
from api.model_registry_v2 import get_registry_v2

registry = get_registry_v2()

# Register new model version
registry.register('ranker', 'v2.1', 'models/xgboost_ranker/ranker_v2.1.json', 
                  {'ndcg': 0.38}, 'xgboost')

# Start experiment
registry.start_experiment('exp_001', 'ranker', 'v2.0', 'ranker', 'v2.1', split=0.5)

# Route requests automatically via user_id hash
model = registry.get_model_for_request(user_id, experiment_id='exp_001')
```

## Cold Start Strategy

1. **New Users**: Mix of popular + trending + genre exploration (30% explore)
2. **New Books**: Content-based boost + author similarity
3. **Sparse Users**: Two-tower embeddings generalize well
4. **Fallback**: Always degrade gracefully to popularity

## Deployment Checklist

- [ ] Redis running (docker-compose up redis)
- [ ] ClickHouse ETL passing (Airflow green)
- [ ] FAISS index built and < 10ms search
- [ ] XGBoost ranker trained and deployed
- [ ] Two-tower model trained and FAISS rebuilt
- [ ] Session model trained (optional Phase 5)
- [ ] Prometheus scraping metrics
- [ ] Java backend pointing to /recommend/v2/
- [ ] A/B test framework configured
- [ ] Rollback plan documented

