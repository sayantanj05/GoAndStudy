# Apriori Association Rules

**Generated:** 2026-04-27 15:06:30

**Method:** Apriori algorithm on loan transactions

**Transactions:** 88 loans from 5 users

**Min support:** 0.200

**Min confidence:** 0.01 (lowered for small dataset)

## Top Association Rules

| If Borrowed (Antecedent) | Then Also Borrowed (Consequent) | Support | Confidence | Lift | Interpretation |
|---|---|---|---|---|---|
| A Journey To The Center of the Earth | Artificial Intelligence: A Modern Approach  | 0.200 | 100.00% | 5.00 | Strong association |
| Artificial Intelligence: A Modern Approach  | A Journey To The Center of the Earth | 0.200 | 100.00% | 5.00 | Strong association |
| Wings of Fire | A Journey To The Center of the Earth | 0.200 | 100.00% | 5.00 | Strong association |
| A Journey To The Center of the Earth | Wings of Fire | 0.200 | 100.00% | 5.00 | Strong association |
| Artificial Intelligence: A Modern Approach  | Wings of Fire | 0.200 | 100.00% | 5.00 | Strong association |
| Wings of Fire | Artificial Intelligence: A Modern Approach  | 0.200 | 100.00% | 5.00 | Strong association |
| Data Mining and Data Warehousing: Principles and Practical Techniques | Fundamentals of Database Management | 0.200 | 100.00% | 5.00 | Strong association |
| Fundamentals of Database Management | Data Mining and Data Warehousing: Principles and Practical Techniques | 0.200 | 100.00% | 5.00 | Strong association |
| Twenty Thousand Leagues Under the Sea | Data Mining and Data Warehousing: Principles and Practical Techniques | 0.200 | 100.00% | 5.00 | Strong association |
| Data Mining and Data Warehousing: Principles and Practical Techniques | Twenty Thousand Leagues Under the Sea | 0.200 | 100.00% | 5.00 | Strong association |
| ISC Computer Science with Java and Bluej | Data Mining and Data Warehousing: Principles and Practical Techniques | 0.200 | 100.00% | 5.00 | Strong association |
| Data Mining and Data Warehousing: Principles and Practical Techniques | ISC Computer Science with Java and Bluej | 0.200 | 100.00% | 5.00 | Strong association |
| Fundamentals of Database Management | ISC Computer Science with Java and Bluej | 0.200 | 100.00% | 5.00 | Strong association |
| ISC Computer Science with Java and Bluej | Fundamentals of Database Management | 0.200 | 100.00% | 5.00 | Strong association |
| Twenty Thousand Leagues Under the Sea | Fundamentals of Database Management | 0.200 | 100.00% | 5.00 | Strong association |
| Fundamentals of Database Management | Twenty Thousand Leagues Under the Sea | 0.200 | 100.00% | 5.00 | Strong association |
| Wings of Fire | Artificial Intelligence: A Modern Approach , A Journey To The Center of the Earth | 0.200 | 100.00% | 5.00 | Strong association |
| Artificial Intelligence: A Modern Approach  | Wings of Fire, A Journey To The Center of the Earth | 0.200 | 100.00% | 5.00 | Strong association |
| Twenty Thousand Leagues Under the Sea | ISC Computer Science with Java and Bluej | 0.200 | 100.00% | 5.00 | Strong association |
| ISC Computer Science with Java and Bluej | Twenty Thousand Leagues Under the Sea | 0.200 | 100.00% | 5.00 | Strong association |

## Interpretation Guide

- **Support**: Fraction of all users who borrowed both books
- **Confidence**: Probability of borrowing consequent given antecedent
- **Lift > 1**: Books are more likely to be borrowed together than by chance
- **Lift > 2**: Strong recommendation candidate

## Business Use Cases

1. **Bundle Recommendations**: Suggest consequent books on antecedent book pages
2. **Shelf Placement**: Place associated books near each other
3. **Email Campaigns**: 'Readers of X also enjoyed Y' personalized emails
4. **Inventory**: Ensure both books are well-stocked together

