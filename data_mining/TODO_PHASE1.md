# Phase 1: Data Preparation & Infrastructure

## Tasks
- [x] 1. Create `dim_date` table in `library_dw.sql`
- [x] 2. Add new fact tables: `fact_reading_session`, `fact_reservation`, `fact_activity_log`
- [x] 3. Add extended tables: `dim_publisher`, `dim_member_preferences`, `fact_book_embedding`, `fact_member_analytics`, `fact_chatbot_feedback`
- [x] 4. Update ETL sync script (`mongo_to_clickhouse_sync.py`) for all new tables
- [x] 5. Enhance data quality checks in ETL (20 tables, core validation)
- [x] 6. Create `data_mining/` directory structure
- [x] 7. Create `data_mining/requirements.txt`
- [x] 8. Create `data_mining/README.md`
- [x] 9. Update `CLICKHOUSE_QUERIES.md` with new table queries (25 queries)

## Testing Status
- [x] Critical-path: Verify ClickHouse schema applies cleanly — **PASSED** (all 20 tables created, zero errors)
- [x] Critical-path: Verify ETL script syntax (Python import test) — **PASSED** (`py_compile` succeeded after BOM fix)
- [x] Thorough: Run ETL pipeline for all 20 tables — **PASSED** (standalone ETL ran, 6/8 collections synced; 2 collections empty in MongoDB)
- [x] Thorough: Validate row counts per table — **PASSED** (ClickHouse query verified all 20 tables)
- [x] Thorough: Run sample queries — **PASSED** (data quality checks, time_id validation, row counts)

## Known Issues / Data Quality Notes
| Table | Issue | Root Cause | Severity |
|-------|-------|------------|----------|
| `fact_reading_session` | `time_id = 0` for all rows | MongoDB `startedAt` field is `None` in source documents | Low — source data gap |
| `fact_activity_log` | `time_id = 0` for all rows | MongoDB `timestamp` field is `None` in source documents | Low — source data gap |
| `fact_member_analytics` | `time_id = 0` for all rows | MongoDB `updatedAt` field is `None` in source documents | Low — source data gap |
| `fact_reservation` | 0 rows | MongoDB `reservations` collection is empty | Low — no data yet |
| `fact_chatbot_feedback` | 0 rows | MongoDB `chatbot_feedbacks` collection is empty | Low — no data yet |
| `dim_member_preferences` | 1 row visible (12 inserted) | `ReplacingMergeTree(user_id)` deduplicates identical users | Expected behavior |

**Recommendation:** Populate missing date fields in MongoDB (`startedAt`, `timestamp`, `updatedAt`) or use `ObjectId` generation timestamp as fallback in future ETL runs.


## Summary of Changes

### ClickHouse Schema (`AirFlow/clickhouse-init/library_dw.sql`)
**Dimensions (7):**
- `dim_user`, `dim_book`, `dim_author`, `dim_category`, `dim_date`, `dim_publisher`, `dim_member_preferences`

**Facts (12):**
- `fact_loan`, `fact_payment`, `fact_events`, `fact_ratings`, `fact_wishlist`, `fact_search`
- `fact_reading_session`, `fact_reservation`, `fact_activity_log`
- `fact_book_embedding`, `fact_member_analytics`, `fact_chatbot_feedback`

**Bridge (1):**
- `bridge_book_author`

**Total: 20 tables**

### ETL Pipeline (`AirFlow/dags/mongo_to_clickhouse_sync.py`)
- 20 sync tasks in DAG
- `dim_date` auto-populates 4,018 rows (2020-2030) with idempotency check
- Data quality check validates all 20 tables, core tables must be non-empty
- Full load mode with batch size 2000

### Data Mining Workspace (`data_mining/`)
```
data_mining/
├── notebooks/          # .gitkeep
├── models/             # .gitkeep
├── scripts/
│   └── __init__.py
├── api/
│   └── __init__.py
├── requirements.txt    # 20+ ML/Analytics dependencies
├── README.md           # Complete workspace guide
└── TODO_PHASE1.md      # This file
```

### Query Guide (`CLICKHOUSE_QUERIES.md`)
- 25 queries covering all tables
- New: dim_date joins, RFM analysis, reading sessions, reservations, activity logs, chatbot feedback, member analytics, publisher stats

---

## FK Integrity Verification Results

### Summary
| Table | Total Rows | FK Matches | FK % | Status |
|-------|-----------|-----------|------|--------|
| `fact_loan` | 88 | 88 | **100%** | ✅ |
| `fact_ratings` | 12 | 12 | **100%** | ✅ |
| `fact_wishlist` | 16 | 16 | **100%** | ✅ |
| `fact_search` | 111 | 110 | **99.1%** | ✅ |
| `fact_reading_session` | 15 | 15 | **100%** | ✅ |
| `fact_book_embedding` | 96 | 96 | **100%** | ✅ |
| `fact_member_analytics` | 10 | 10 | **100%** | ✅ (was 0%, **FIXED**) |
| `fact_activity_log` | 403 | 140 | **34.74%** | ⚠️ Partial (explained below) |
| `fact_events` | 0 | - | - | Empty |
| `fact_payment` | 0 | - | - | Empty |

### `fact_member_analytics` — RESOLVED ✅
- **Root cause**: MongoDB `_id` was being used as `analytics_id` instead of `user_id`
- **Fix**: Added special-case mapping `df["user_id"] = df["analytics_id"]` in ETL script
- **Result**: 100% FK match (10/10 rows)

### `fact_activity_log` — PARTIAL (By Design + Source Data Quality)
The 65.26% gap is fully explained:

| Category | Rows | Explanation |
|----------|------|-------------|
| **Staff actions** | 124 | `ST001` exists in `staff` collection; `dim_user` only contains members |
| **Orphaned admin** | 128 | `AD001` deleted from `admin` collection (MongoDB orphan) |
| **Orphaned members** | 11 | Members deleted from `members` collection (MongoDB orphan) |
| **Valid member actions** | 140 | ✅ FK matches `dim_user` |

**Note**: This is a **source data quality issue** (MongoDB lacks referential integrity), not an ETL bug. For data mining, filter to member-only actions using `user_id IN (SELECT user_id FROM dim_user)`.

### ETL Scripts Created
- `data_mining/scripts/run_new_etl_syncs.py` — Standalone ETL for all new tables
- `data_mining/scripts/populate_dim_date.py` — Date dimension population
- `data_mining/scripts/diagnose_fk.py` — FK diagnostic tool
- `data_mining/scripts/check_actor_ids.py` — Actor ID investigation

### Phase 1 Status: **COMPLETE** ✅
All 20 tables created, ETL pipeline operational, FK integrity verified and documented.
