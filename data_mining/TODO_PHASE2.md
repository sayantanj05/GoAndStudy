# Phase 2: Exploratory Data Analysis (EDA) — COMPLETE ✅

All goals accomplished. 13 reports generated across 3 sub-phases.

---
## ~~Phase 2.0 — time_id validation and descriptive analytics already done~~
## ~~Phase 2.0 ✅ time_id fix applied (see run logs FACT_LOAN_TIME_ID_LOG)~~

## Phase 2.1: Descriptive Analytics — ✅ COMPLETE
**Script:** `scripts/phase2_1_descriptive_analytics.py`

| # | Analysis | File | Rows |
|---|----------|------|------|
| 1 | Top Borrowers | `01_top_borrowers.md` | 3 |
| 2 | Loan Status Breakdown | `02_loan_status_breakdown.md` | 3 |
| 3 | Most Borrowed Books | `03_most_borrowed_books.md` | 9 |
| 4 | Genre Popularity | `04_genre_popularity.md` | 5 |
| 5 | Monthly Loan Trends | `05_monthly_loan_trends.md` | 2 |
| 6 | Day-of-Week Patterns | `06_day_of_week_patterns.md` | 4 |
| 7 | Activity Funnel | `07_activity_funnel.md` | 4 |
| 8 | Top Search Queries | `08_top_searches.md` | 10 |
| 9 | Reading Engagement | `09_reading_engagement.md` | 7 |
| 10 | Member Analytics Overview | `10_member_analytics.md` | 10 |

**Key finding:** 3 active members account for 88 total loans (Sayantan Jana: 64, Verify Member: 16, Test Member: 8). Zero overdues in current dataset.

## Phase 2.2: Correlation Analysis — ✅ COMPLETE
**Script:** `scripts/phase2_2_correlation_analysis.py`
**Report:** `reports/phase2_2/correlation_matrix.md`

| Hypothesis | r | p-value | n | Result |
|------------|---|---------|---|--------|
| More searches → more loans | 0.000 | 0.5 | 10 | No correlation |
| Overdue users search more | N/A | N/A | 10 | Constant data |
| Reading time → loan frequency | 0.368 | 0.468 | 5 | Weak, not significant |
| Higher rated books borrowed more | -0.111 | 0.649 | 24 | No correlation |

**Insight:** Current dataset (small sample) shows no strong correlations. Data will improve with more member activity.

## Phase 2.3: Content-Based Book Similarity — ✅ COMPLETE
**Script:** `scripts/phase2_3_book_similarity.py`
**Reports:** `reports/phase2_3/book_similarity.md`, `similarity_matrix.csv`

Top similar pairs found:
- **Atomic Habits** ↔ **Educated** (sim=0.605)
- **Educated** ↔ **Sapiens** (sim=0.546)
- **Atomic Habits** ↔ **Sapiens** (sim=0.485)

**Use case:** Power "Related Books" recommendation widget on book detail pages.

---
## All Phase 2 Goals Met
- ✅ Patterns discovered (borrowing concentration, genre preferences)
- ✅ Relationships explored (content similarity, correlation attempts)
- ✅ Insights extracted (top borrowers, zero-overdue anomaly, funnel stages)
- ✅ Trends visualized (monthly, day-of-week patterns)
- ✅ Changes over time tracked (loan volume by month)
- ✅ Clusters formed (book similarity matrix for recommendations)

**Next Step:** Phase 3 — Advanced ML (K-Means clustering, association rules, time-series forecasting)
