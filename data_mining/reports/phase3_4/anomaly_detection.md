# Anomaly Detection Report

**Generated:** 2026-04-27 15:17:02

**Method:** Isolation Forest (unsupervised)

**Users analyzed:** 10

**Anomalies detected:** 1 (10.0%)

**Contamination rate:** 0.100

## Anomalous Users

| Name | Email | Loans | Overdue | Fines | Searches | Wishlist | Reviews | Avg Rating | Reason |
|---|---|---|---|---|---|---|---|---|---|
| Sayantan Jana | sayantanj05@gmail.com | 8 | 0 | 0.00 | 43 | 1 | 3 | 5.0 | High loan volume; Excessive searches |

## Normal User Statistics

| Metric | Mean | Median | 95th Percentile |
|---|---|---|---|
| total_loans | 2.11 | 1.00 | 5.20 |
| overdue_loans | 0.00 | 0.00 | 0.00 |
| total_fines | 0.00 | 0.00 | 0.00 |
| total_searches | 8.11 | 1.00 | 38.40 |
| wishlist_items | 1.22 | 1.00 | 2.20 |
| total_reviews | 1.00 | 1.00 | 1.00 |
| avg_rating | 0.00 | 0.00 | 0.00 |

## Business Actions

1. **Review flagged users**: Check for account sharing or bot behavior
2. **High loan volume**: Verify if legitimate power user or suspicious
3. **Many overdue**: Send reminders, consider account restrictions
4. **Excessive searches**: Possible scraping — monitor IP patterns
5. **No engagement**: Inactive accounts — consider re-engagement campaigns

