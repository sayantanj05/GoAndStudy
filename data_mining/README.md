# GoAndStudy Library — Data Mining & Analytics Workspace

This directory contains all data mining, machine learning, and analytics code for the GoAndStudy Library Management System.

## Directory Structure

```
data_mining/
├── notebooks/          # Jupyter notebooks for EDA and experimentation
│   ├── 01_eda_and_profiling.ipynb
│   ├── 02_rfm_analysis.ipynb
│   ├── 03_user_clustering.ipynb
│   ├── 04_association_rules.ipynb
│   ├── 05_time_series_forecasting.ipynb
│   └── 06_recommendation_engine.ipynb
├── models/             # Serialized trained models (.pkl, .joblib)
│   ├── churn_model/
│   ├── recommendation_model/
│   └── forecasting_model/
├── scripts/            # Reusable Python scripts for batch processing
│   ├── train_churn_model.py
│   ├── train_recommender.py
│   ├── generate_insights.py
│   └── evaluate_models.py
├── api/                # FastAPI prediction service
│   ├── main.py
│   ├── models.py
│   └── routers/
└── requirements.txt    # Python dependencies
```

## Phase 1: Infrastructure (Current)

- [x] `dim_date` table created in ClickHouse
- [x] New fact tables: `fact_reading_session`, `fact_reservation`, `fact_activity_log`
- [x] ETL pipeline enhanced to sync new tables
- [x] Data quality checks expanded to 15 tables

## Phase 2: Exploratory Data Analysis (Next)

Run these notebooks in order:

1. **01_eda_and_profiling.ipynb** — Data quality, distributions, correlations
2. **02_rfm_analysis.ipynb** — Member segmentation via Recency, Frequency, Monetary

## Phase 3: Advanced Algorithms (Upcoming)

| Notebook | Algorithm | Goal |
|----------|-----------|------|
| `03_user_clustering.ipynb` | K-Means, DBSCAN | Segment members by behavior |
| `04_association_rules.ipynb` | Apriori, FP-Growth | "Borrowed together" patterns |
| `05_time_series_forecasting.ipynb` | Prophet, ARIMA | Loan volume forecasting |
| `06_recommendation_engine.ipynb` | Collaborative + Content-based | Book recommendations |

## Quick Start

```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Connect to ClickHouse
python -c "import clickhouse_connect; client = clickhouse_connect.get_client(host='localhost', port=8123, database='library_dw'); print(client.query('SHOW TABLES').result_rows)"

# 3. Launch Jupyter
jupyter notebook notebooks/
```

## ClickHouse Connection

```python
import clickhouse_connect
client = clickhouse_connect.get_client(
    host='localhost',
    port=8123,
    database='library_dw'
)
```

## Environment Variables

Create a `.env` file in this directory:

```
CLICKHOUSE_HOST=localhost
CLICKHOUSE_PORT=8123
CLICKHOUSE_DB=library_dw
MONGO_URI=mongodb+srv://...
MONGO_DB_NAME=goandstudydb
```

## Model Registry

Trained models are versioned with timestamps:

```
models/churn_model/
  ├── churn_20240115_143022.joblib
  ├── churn_20240115_143022_metadata.json
  └── latest.joblib -> churn_20240115_143022.joblib
```

## API Service

Start the prediction API:

```bash
cd api/
uvicorn main:app --reload --port 8001
```

Endpoints:
- `POST /predict/churn` — Member churn probability
- `GET /recommend/{user_id}` — Personalized book recommendations
- `GET /forecast/loans` — Next 30-day loan volume forecast
