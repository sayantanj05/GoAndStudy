# Churn Prediction Report

**Generated:** 2026-04-27 18:26:25

**Members analyzed:** 10

**Method:** Engagement-based scoring + Random Forest (if classes ≥ 2)

## Churn Risk Summary

| Name | Loans | Searches | Wishlist | Days Idle | Churn Probability | Risk Level |
|---|---|---|---|---|---|---|
| Verify Member | 4 | 1 | 1 | 29 | 91.0% | 🔴 High |
| Sayantan Jana | 2 | 3 | 1 | 29 | 83.0% | 🔴 High |
| Test Member | 2 | 1 | 1 | 22 | 74.0% | 🔴 High |
| Sayantan Jana | 6 | 62 | 3 | 29 | 24.0% | 🟢 Low |
| Sayantan Jana | 8 | 43 | 1 | 17 | 18.0% | 🟢 Low |
| Test User Login | 1 | 1 | 1 | 0 | 0.0% | 🟢 Low |
| Test User | 1 | 1 | 1 | 0 | 0.0% | 🟢 Low |
| Test User | 1 | 1 | 1 | 0 | 0.0% | 🟢 Low |
| Arijit Das | 1 | 2 | 1 | 0 | 0.0% | 🟢 Low |
| Test User Five | 1 | 1 | 1 | 0 | 0.0% | 🟢 Low |

## Feature Importance

| Feature | Importance |
|---|---|
| total_loans | 0.413 |
| days_since_last_loan | 0.314 |
| search_count | 0.173 |
| wishlist_count | 0.058 |
| avg_rating | 0.023 |
| review_count | 0.018 |
| total_fines | 0.000 |
| overdue_count | 0.000 |

## Recommendations

- **High Risk (≥70%)**: Immediate re-engagement email with personalized book suggestions
- **Medium Risk (40-69%)**: Targeted newsletter, loyalty rewards reminder
- **Low Risk (<40%)**: Maintain engagement with new arrivals notifications

