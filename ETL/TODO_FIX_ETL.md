# ETL Pipeline Fix — TODO Tracker

## Root Causes Identified
1. `mongo_to_clickhouse_sync.py` — ClickHouse client instantiated at module level → DAG import crashes
2. `mongo_to_clickhouse_dag.py` — `MONGO_URI` missing from environment at runtime
3. `mongo_to_clickhouse_sync_backup.py` — Uses deprecated Airflow 3.x API (`schedule_interval`, old import)
4. `docker-compose.yaml` — No ClickHouse service defined
5. **NEW**: `library_dw` database had **zero tables** — ETL expected `dim_user`, `dim_book`, etc. but schema was never created

## Steps

- [x] Step 1: Add ClickHouse service to `docker-compose.yaml`
- [x] Step 2: Fix main ETL DAG (`mongo_to_clickhouse_sync.py`) — lazy client init, env-based config
- [x] Step 3: Fix simple sync DAG (`mongo_to_clickhouse_dag.py`) — proper `.env` loading, lazy connections
- [x] Step 4: Fix backup DAG (`mongo_to_clickhouse_sync_backup.py`) — Airflow 3.x compatibility
- [x] Step 5: Update `requirements.txt` with latest versions
- [x] Step 6: Update `.env.example` with all required variables
- [x] Step 7: Create ClickHouse schema DDL (`clickhouse-init/library_dw.sql`)
- [x] Step 8: Update insert helpers to use `pd.NA` instead of `fillna('')` for missing columns
- [x] Step 9: Mount init scripts into docker-compose
- [ ] Step 10: **Create tables in the running ClickHouse container**
- [ ] Step 11: Rebuild Docker images and restart stack
- [ ] Step 12: Verify DAGs load without import errors
- [ ] Step 13: Trigger test run and validate data sync

## Immediate Action Required: Create Tables

Since your ClickHouse container is already running, the init scripts won't auto-run (they only execute on first startup with an empty volume).

**Option A — Fastest: Run SQL manually in your open clickhouse-client session**

Copy-paste this entire block into your `af6ae7da651a :)` prompt and press Enter:

```sql
CREATE DATABASE IF NOT EXISTS library_dw;

CREATE TABLE IF NOT EXISTS library_dw.dim_user (
    user_id String, name String, email String, phone String,
    role String, membership_type String, preferred_genres Array(String),
    total_loans Nullable(Int32), active_loans Nullable(Int32),
    created_at Nullable(DateTime), updated_at Nullable(DateTime)
) ENGINE = ReplacingMergeTree(updated_at) ORDER BY user_id;

CREATE TABLE IF NOT EXISTS library_dw.dim_book (
    book_id String, isbn String, title String,
    author_ids Array(String), category_ids Array(String), description String,
    average_rating Nullable(Float64), total_ratings Nullable(Int32),
    total_issues Nullable(Int32), available_copies Nullable(Int32),
    created_at Nullable(DateTime), updated_at Nullable(DateTime)
) ENGINE = ReplacingMergeTree(updated_at) ORDER BY isbn;

CREATE TABLE IF NOT EXISTS library_dw.dim_author (
    author_id String, name String
) ENGINE = ReplacingMergeTree() ORDER BY author_id;

CREATE TABLE IF NOT EXISTS library_dw.dim_category (
    category_id String, name String
) ENGINE = ReplacingMergeTree() ORDER BY category_id;

CREATE TABLE IF NOT EXISTS library_dw.bridge_book_author (
    book_id String, author_id String
) ENGINE = MergeTree() ORDER BY (book_id, author_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_loan (
    loan_id String, user_id String, book_id String, isbn String, status String,
    issued_at Nullable(DateTime), due_date Nullable(DateTime), returned_at Nullable(DateTime),
    renewal_count Nullable(Int32), fine_amount Nullable(Float64), overdue_days Nullable(Int32),
    created_at Nullable(DateTime), time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, loan_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_payment (
    payment_id String, user_id String, loan_id String,
    amount Nullable(Float64), payment_time Nullable(DateTime), time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, payment_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_events (
    event_id String, loan_id String, event_type String,
    timestamp Nullable(DateTime), triggered_by String, time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, event_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_ratings (
    review_id String, user_id String, book_id String,
    rating Nullable(Int32), review_text String,
    created_at Nullable(DateTime), time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, review_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_wishlist (
    wishlist_id String, user_id String, book_id String,
    added_at Nullable(DateTime), time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, wishlist_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_search (
    search_id String, user_id String, query String,
    created_at Nullable(DateTime), time_id Int32
) ENGINE = MergeTree() ORDER BY (time_id, search_id);
```

Then verify:
```sql
SHOW TABLES FROM library_dw;
```

You should see 11 tables listed.

---

**Option B — For fresh deployments (not needed now):**
```bash
cd AirFlow
docker-compose down
docker volume rm airflow_clickhouse-data  # WARNING: deletes all ClickHouse data
docker-compose up -d
```
The init script at `clickhouse-init/library_dw.sql` will auto-run on first startup.

## Next Steps After Tables Exist

1. **Ensure your `.env` file exists with a real `MONGO_URI`**
   ```bash
   cd AirFlow
   cp .env.example .env
   # Edit .env and paste your MongoDB connection string
   ```

2. **Restart Airflow to pick up the fixed DAGs:**
   ```bash
   docker-compose restart airflow-dag-processor airflow-scheduler airflow-worker
   ```

3. **Open Airflow UI** at http://localhost:8080 (default login: `airflow` / `airflow`)

4. **Unpause and trigger** the `mongodb_clickhouse_library_etl` DAG

5. **Monitor the run** — check task logs for each dimension and fact table

6. **Verify data in ClickHouse:**
   ```bash
   docker exec -it airflow-clickhouse-1 clickhouse-client --query "SELECT 'dim_user' as t, count() as rows FROM library_dw.dim_user UNION ALL SELECT 'dim_book', count() FROM library_dw.dim_book UNION ALL SELECT 'fact_loan', count() FROM library_dw.fact_loan"
   ```

## Key Changes Summary

| File | What Changed |
|---|---|
| `docker-compose.yaml` | Added `clickhouse` service + mounted `clickhouse-init/` for auto-schema creation |
| `mongo_to_clickhouse_sync.py` | Lazy client init, `os.getenv` instead of Airflow Variables, Airflow 3.x imports, `schedule` param, **fixed `_insert_batch` to use `pd.NA` for missing columns instead of `fillna('')`** |
| `mongo_to_clickhouse_dag.py` | Explicit `.env` loading, `os.getenv` defaults for Docker network |
| `mongo_to_clickhouse_sync_backup.py` | Updated imports (`providers.standard.operators.python`), replaced `schedule_interval` with `schedule`, lazy init |
| `requirements.txt` | Pinned modern versions |
| `.env.example` | New template with all required variables |
| `clickhouse-init/library_dw.sql` | **NEW** — Complete DDL for all dimension and fact tables with `Nullable` types for safe ETL loading |

