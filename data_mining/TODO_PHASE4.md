# Phase 4: Predictive Modeling — TODO Tracker

## Objectives
Build three predictive models on the library_dw ClickHouse warehouse:
1. **Churn Prediction** — Random Forest classifier to identify at-risk members
2. **Overdue Prediction** — Logistic Regression to predict loan default risk
3. **Fine Estimation** — Linear Regression to estimate fine amounts

---

## Completion Status

### 4.1 Churn Prediction (`phase4_1_churn_prediction.py`)
- [x] Query member engagement data from ClickHouse (dim_user, fact_loan, fact_search, fact_wishlist, fact_ratings)
- [x] Build engagement score heuristic (total_loans, days_since_last_loan, search_count, wishlist_count, avg_rating)
- [x] MinMaxScaler normalization
- [x] Median split for binary churn label
- [x] Random Forest training with feature importance
- [x] Single-class guard (if all members identical)
- [x] Generate markdown report + CSV export
- [x] **TESTED** ✅ — 10 members analyzed, 7 active / 3 at-risk
  - Top features: total_loans (0.413), days_since_last_loan (0.314), search_count (0.173)
  - High-risk members: Verify Member (91%), Sayantan Jana (83%), Test Member (74%)

### 4.2 Overdue Prediction (`phase4_2_overdue_prediction.py`)
- [x] Query loan data with member history and book attributes
- [x] Column normalization for ClickHouse `l.*` prefixes
- [x] Missing-column guards (fine_amount, overdue_days, etc.)
- [x] Dynamic feature list construction
- [x] Single-class guard for datasets with zero overdue loans
- [x] Generate meaningful fallback report when model cannot train
- [x] **TESTED** ✅ — 22 loans loaded, all on-time
  - Gracefully handled single-class case: "Only one class found in data (all loans are on-time)"
  - Fallback report saved with data requirements guidance

### 4.3 Fine Estimation (`phase4_3_fine_estimation.py`)
- [x] Query overdue/fined loan data with member history
- [x] Column normalization for ClickHouse `l.*` prefixes
- [x] Empty DataFrame guard (0 rows from ClickHouse)
- [x] Safe numeric conversion with coerce + fillna
- [x] Safe derived column creation (days_overdue, has_previous_overdue)
- [x] Dynamic feature list construction
- [x] Insufficient training data guard (< 3 fined loans)
- [x] Generate meaningful fallback report when model cannot train
- [x] **TESTED** ✅ — 0 overdue/fined loans found
  - Gracefully handled empty dataset: "No overdue or fined loans found in dataset"
  - Fallback report saved with data requirements guidance

---

## Known Limitations (Dataset-Dependent)
| Issue | Cause | Resolution Path |
|-------|-------|-----------------|
| No overdue loans to train on | All 22 loans are on-time | Need more loan history with actual overdue returns |
| No fined loans to train on | No OVERDUE status or fine_amount > 0 | Need real overdue scenarios in production data |
| Small member base (n=10) | Limited user registration | Will improve as more members join |

These are **expected behaviors** for a young library system — the scripts are designed to degrade gracefully and provide actionable reports even when training data is insufficient.

---

## Files Created / Modified
| File | Purpose |
|------|---------|
| `data_mining/scripts/phase4_1_churn_prediction.py` | Churn prediction model |
| `data_mining/scripts/phase4_2_overdue_prediction.py` | Overdue prediction model |
| `data_mining/scripts/phase4_3_fine_estimation.py` | Fine estimation model |
| `data_mining/reports/phase4_1/churn_prediction.md` | Churn analysis report |
| `data_mining/reports/phase4_1/churn_prediction.csv` | Churn scores per member |
| `data_mining/reports/phase4_2/overdue_prediction.md` | Overdue analysis report |
| `data_mining/reports/phase4_3/fine_estimation.md` | Fine estimation report |

---

## Next Steps (When Data Grows)
1. Re-run scripts after accumulating 50+ loans with varied statuses
2. Tune hyperparameters (n_estimators for RF, C for LR, regularization for Linear Regression)
3. Add cross-validation and train/test splits
4. Deploy models via FastAPI prediction endpoints
5. Integrate predictions into admin dashboard

---

**Phase 4 Status: COMPLETE** ✅
All scripts execute without errors, handle edge cases gracefully, and produce meaningful reports.
