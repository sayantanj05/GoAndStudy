# GoAndStudy: ML vs Data Mining Architecture Guide

## Your Core Question

> "We applied Data Mining Algorithms on Data Warehouse to find trends, patterns, relationships, clusters etc. So where will we apply ML algorithms?"

**Short Answer**: Data Mining discovers patterns *offline* from historical data. ML algorithms *predict* outcomes *online* for real-time user requests. They are complementary layers in the same system.

---

## Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER (React)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Member Pages │  │ Admin Pages  │  │ AI Chatbot   │  │ Analytics   │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘ │
└─────────┼─────────────────┼─────────────────┼─────────────────┼─────────┘
          │                 │                 │                 │
          ▼                 ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      JAVA BACKEND (Spring Boot)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ REST APIs    │  │ AiRecommen-  │  │ Auth/Loan/   │  │ ML Service  │ │
│  │ Controllers  │◀─┤ dationService │  │ Book Services│  │ Client      │ │
│  └──────────────┘  └──────┬───────┘  └──────────────┘  └──────┬──────┘ │
│                           │                                    │         │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │  getPrecomputedRecommendations() now calls ML service first  │       │
│  │  Fallback: vector search / popularity if ML unavailable      │       │
│  └──────────────────────────────────────────────────────────────┘       │
└─────────────────────────┬──────────────────────┬────────────────────────┘
                          │                      │
              ┌───────────┘                      └───────────┐
              ▼                                              ▼
┌─────────────────────────────────┐            ┌──────────────────────────┐
│   DATA MINING (Batch/Offline)   │            │   ML SERVICE (Real-time) │
│   `data_mining/scripts/`        │            │   `ML/`                  │
│                                 │            │                          │
│  ┌─────────────────────────┐    │            │  ┌──────────────────┐    │
│  │ Phase 2: Descriptive    │    │            │  │ FastAPI (Port    │    │
│  │ - Top borrowers         │    │            │  │ 8001)            │    │
│  │ - Loan status breakdown │    │            │  │                  │    │
│  │ - Correlation analysis  │    │            │  │  ┌────────────┐  │    │
│  └─────────────────────────┘    │            │  │  │ /api/v1/   │  │    │
│                                 │            │  │  │ recommend/ │  │    │
│  ┌─────────────────────────┐    │            │  │  │ {user_id}  │  │    │
│  │ Phase 3: Pattern Discovery│   │            │  │  └────────────┘  │    │
│  │ - K-Means clustering    │    │            │  │                  │    │
│  │   (member segments)     │    │            │  │  ┌────────────┐  │    │
│  │ - Apriori rules         │    │            │  │  │ /api/v1/   │  │    │
│  │   (borrowed together)   │    │            │  │  │ health     │  │    │
│  │ - Isolation Forest      │    │            │  │  └────────────┘  │    │
│  │   (anomaly detection)   │    │            │  │                  │    │
│  │ - Prophet forecasting   │    │            │  └──────────────────┘    │
│  │   (loan volume)         │    │            │                          │
│  └─────────────────────────┘    │            │  Input: ClickHouse DW    │
│                                 │            │  Output: Predicted books │
│  ┌─────────────────────────┐    │            │                          │
│  │ Phase 4: Predictive     │    │            │  Algorithm: NMF          │
│  │ - Churn prediction      │    │            │  (Non-negative Matrix    │
│  │   (Random Forest)       │◄───┼────────────┼──┤ Factorization)         │
│  │ - Overdue prediction    │    │            │  Interim before SVD++    │
│  │ - Fine estimation       │    │            │  at 500+ users           │
│  └─────────────────────────┘    │            └──────────────────────────┘
│                                 │
│  Common Pattern:                │
│  1. Query ClickHouse DW         │
│  2. Run algorithm               │
│  3. Generate report (MD/CSV)    │
│  4. Save to data_mining/reports/│
│                                 │
│  Triggered by: Airflow DAG or   │
│  manual execution               │
│                                 │
└─────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATA WAREHOUSE (ClickHouse)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐ │
│  │ dim_book │  │ dim_user │  │ fact_loan│  │ fact_ratings   │ │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────┘ │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐ │
│  │fact_search│ │fact_wishl│  │fact_activ│  │ fact_member_   │ │
│  │          │  │ist       │  │ity_log   │  │ analytics      │ │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          ▲
          │
┌─────────────────────────────────────────────────────────────────┐
│                     SOURCE DATABASE (MongoDB)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐ │
│  │ books    │  │ members  │  │ loans    │  │ book_reviews   │ │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────┘ │
│                    ↑ ETL Pipeline (Airflow)                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Where Each Algorithm Lives

### Data Mining Algorithms (Batch/Offline)

| Script | Algorithm | What It Does | Where Output Goes |
|--------|-----------|-------------|-------------------|
| `phase2_1_descriptive_analytics.py` | SQL Aggregation | Top borrowers, status breakdown | `data_mining/reports/` |
| `phase2_2_correlation_analysis.py` | Pearson/Spearman | Feature correlations | `data_mining/reports/` |
| `phase2_3_book_similarity.py` | Cosine Similarity | Content-based book similarity | `data_mining/reports/` |
| `phase3_1_kmeans_clustering.py` | K-Means | Member segmentation (Power/Regular/Casual) | `data_mining/reports/` |
| `phase3_2_apriori_rules.py` | Apriori | "Borrowed together" association rules | `data_mining/reports/` |
| `phase3_3_prophet_forecast.py` | Prophet | Loan volume time-series forecasting | `data_mining/reports/` |
| `phase3_4_isolation_forest.py` | Isolation Forest | Anomaly detection (fraudulent activity) | `data_mining/reports/` |
| `phase4_1_churn_prediction.py` | Random Forest | Predict churn probability | `data_mining/reports/` |
| `phase4_2_overdue_prediction.py` | Logistic Regression | Predict overdue risk | `data_mining/reports/` |
| `phase4_3_fine_estimation.py` | Linear Regression | Estimate fine amount | `data_mining/reports/` |

**Key Characteristics**:
- Runs on schedule (Airflow DAG) or manually
- Processes ALL historical data
- Generates static reports for admins
- No direct user interaction

---

### ML Algorithms (Real-time/Online)

| Component | Algorithm | What It Does | User Impact |
|-----------|-----------|-------------|-------------|
| `ML/api/routers/recommend.py` | NMF (currently) | Predict books a user will like | Personalized "For You" section |
| `ML/scripts/train_recommender.py` | SVD++ (future at 500+ users) | Train collaborative filtering model | Better recommendations |
| `ML/api/routers/recommend.py` | Fallback: Content + Popular | Cold-start / offline fallback | Always returns recommendations |

**Key Characteristics**:
- Serves individual user requests in <100ms
- Called by Java backend via RestTemplate
- Model is pre-trained, loaded at startup
- Can fallback to static rules if model unavailable

---

## The Critical Difference

| Aspect | Data Mining | ML Service |
|--------|------------|------------|
| **Trigger** | Scheduled batch / Manual | Per-user HTTP request |
| **Latency** | Minutes to hours | <100 milliseconds |
| **Audience** | Admin dashboards | End users (members) |
| **Output** | Reports, insights, patterns | Predictions, recommendations |
| **Data scope** | All historical data | Specific user's context |
| **Actionability** | Inform decisions | Directly serve content |

---

## Data Flow for Book Recommendations

### Data Mining Path (Offline)
```
MongoDB ──ETL──▶ ClickHouse DW ──▶ K-Means Clustering
                                    ↓
                              "User is Power Reader"
                                    ↓
                              Admin sees segment
                              in dashboard
```

### ML Path (Real-time)
```
User opens "For You" page
        ↓
React ──▶ Java Backend GET /api/ai/precomputed
        ↓
AiRecommendationService.getPrecomputedRecommendations()
        ↓
┌─────────────────────────────────────┐
│ 1. Call ML Service (localhost:8001) │
│    GET /api/v1/recommend/{user_id}  │
│    Returns: predicted_rating scores │
│                                     │
│ 2. If ML unavailable:               │
│    Fallback to vector search /      │
│    popularity-based recommendations │
└─────────────────────────────────────┘
        ↓
Java Backend enriches with book details
        ↓
Returns JSON to React
        ↓
User sees personalized book cards
```

---

## Why Both Are Needed

1. **Data Mining informs strategy**: "Power Readers borrow 5x more than Casual users → create premium membership tier"

2. **ML executes strategy**: "For this specific user, predict they'll love 'War and Peace' with 4.06/5 rating → show it first"

3. **Data Mining validates ML**: Run K-Means on ClickHouse DW to see if ML recommendations actually match cluster preferences

4. **ML feeds back to Data Mining**: Log all ML predictions to ClickHouse `fact_activity_log`, retrain models, run new association rules

---

## Files Created/Modified

### New ML Service
| File | Purpose |
|------|---------|
| `ML/api/main.py` | FastAPI app entry point |
| `ML/api/routers/health.py` | Health check endpoint |
| `ML/api/routers/recommend.py` | Recommendation endpoint with ClickHouse + fallback |
| `ML/api/model_registry.py` | Loads trained model at startup |
| `ML/scripts/recommender_model.py` | NMFRecommender class |
| `ML/scripts/train_recommender.py` | Training script |
| `ML/scripts/data_loader.py` | ClickHouse data fetching |
| `ML/config/config.yaml` | Service configuration |

### Java Backend Integration
| File | Purpose |
|------|---------|
| `back/src/.../MlRecommendationResponse.java` | DTO for ML service response |
| `back/src/.../MlRecommendationService.java` | RestTemplate client to ML service |
| `back/src/.../AiRecommendationService.java` | Modified to call ML first, fallback to existing logic |

---

## Running the System

```bash
# 1. Start ClickHouse (if not running)
cd AirFlow
docker-compose up -d clickhouse

# 2. Start ML Service (in one terminal)
cd ML
python -m uvicorn api.main:app --host 0.0.0.0 --port 8001

# 3. Start Java Backend (in another terminal)
cd back
mvn spring-boot:run

# 4. Access API docs
http://localhost:8001/docs          # ML Service Swagger
http://localhost:8080/swagger-ui    # Java Backend Swagger
```

---

## Next Steps (Future Work)

1. **Train on real data**: Run `ML/scripts/train_recommender.py` when ClickHouse has sufficient loan/rating data
2. **Add feedback loop**: Log ML predictions to `fact_activity_log`, measure CTR
3. **Hybrid model**: Combine collaborative filtering (NMF/SVD++) + content-based (embeddings) + association rules
4. **Scale to SVD++**: Switch to scikit-surprise SVD++ when 500+ users have ratings
5. **Deep learning**: TensorFlow Neural CF at 1000+ users
6. **A/B testing**: Compare ML recommendations vs popularity baseline

---

## Summary

| Question | Answer |
|----------|--------|
| Where is Data Mining? | `data_mining/scripts/` — batch analysis of ClickHouse DW |
| Where is ML? | `ML/` — real-time prediction microservice on port 8001 |
| What does Data Mining output? | Reports, patterns, segments for admins |
| What does ML output? | Personalized predictions for each user |
| How do they connect? | Both read from ClickHouse DW; ML logs to DW for Data Mining validation |
