"""
Phase 4.1: Churn Prediction
Predicts which members are at risk of churning (becoming inactive).
"""

import os
import clickhouse_connect
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import MinMaxScaler
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase4_1"
os.makedirs(REPORTS_DIR, exist_ok=True)

def get_ch_client():
    return clickhouse_connect.get_client(
        host='localhost', port=8123,
        username='default', password='',
        database='library_dw'
    )

def run_query(query: str):
    ch = get_ch_client()
    try:
        result = ch.query(query)
        return result.result_rows, result.column_names
    finally:
        ch.close()

def main():
    print("=" * 60)
    print("PHASE 4.1: CHURN PREDICTION")
    print("=" * 60)
    
    print("\nFetching member data...")
    rows, cols = run_query("""
        SELECT 
            d.user_id,
            d.name,
            COUNT(DISTINCT l.loan_id) AS total_loans,
            COUNT(DISTINCT CASE WHEN l.status = 'OVERDUE' THEN l.loan_id END) AS overdue_count,
            SUM(l.fine_amount) AS total_fines,
            COUNT(DISTINCT s.search_id) AS search_count,
            COUNT(DISTINCT w.wishlist_id) AS wishlist_count,
            COUNT(DISTINCT r.review_id) AS review_count,
            AVG(r.rating) AS avg_rating,
            dateDiff('day', MAX(l.issued_at), today()) AS days_since_last_loan
        FROM dim_user d
        LEFT JOIN fact_loan l ON d.user_id = l.user_id
        LEFT JOIN fact_search s ON d.user_id = s.user_id
        LEFT JOIN fact_wishlist w ON d.user_id = w.user_id
        LEFT JOIN fact_ratings r ON d.user_id = r.user_id
        GROUP BY d.user_id, d.name
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    print(f"Loaded {len(df)} members")
    
    # Handle missing values
    numeric_cols = ['total_loans', 'overdue_count', 'total_fines', 
                    'search_count', 'wishlist_count', 'review_count', 'avg_rating', 'days_since_last_loan']
    for col in numeric_cols:
        df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
    
    # Engagement score: higher = more engaged = lower churn risk
    df['engagement_score'] = (
        df['total_loans'] * 3 +
        df['search_count'] * 1 +
        df['wishlist_count'] * 2 +
        df['review_count'] * 2 +
        df['avg_rating'].fillna(0) * 0.5 -
        df['overdue_count'] * 5 -
        df['days_since_last_loan'] * 0.5
    )
    
    # Normalize engagement to 0-1, then invert for churn score
    scaler = MinMaxScaler()
    df['engagement_norm'] = scaler.fit_transform(df[['engagement_score']])
    df['churn_score'] = 1 - df['engagement_norm']  # Higher = more likely to churn
    
    # Binary churn: top 50% by churn_score
    median_churn = df['churn_score'].median()
    df['churned'] = (df['churn_score'] > median_churn).astype(int)
    
    print(f"\nChurn distribution:")
    print(f"  Active: {len(df[df['churned'] == 0])}")
    print(f"  At-risk: {len(df[df['churned'] == 1])}")
    
    # Features
    features = ['total_loans', 'overdue_count', 'total_fines', 
                'search_count', 'wishlist_count', 'review_count', 'avg_rating', 'days_since_last_loan']
    X = df[features]
    y = df['churned']
    
    unique_classes = np.unique(y)
    
    if len(unique_classes) == 1 or len(df) < 6:
        print(f"\n⚠️ Insufficient class diversity for ML model (N={len(df)}, classes={len(unique_classes)})")
        print("   Using engagement-based heuristic churn probability...")
        df['churn_probability'] = df['churn_score']
        df['prediction'] = df['churned']
        
        # Feature importance from correlation with churn score
        importance = pd.DataFrame({
            'feature': features,
            'importance': [abs(df[f].corr(df['churn_score'])) if df[f].std() > 0 else 0 for f in features]
        }).sort_values('importance', ascending=False)
    else:
        print("\nTraining Random Forest...")
        model = RandomForestClassifier(n_estimators=100, random_state=42, max_depth=5)
        model.fit(X, y)
        
        proba = model.predict_proba(X)
        if proba.shape[1] == 1:
            df['churn_probability'] = float(unique_classes[0])
        else:
            df['churn_probability'] = proba[:, 1]
        df['prediction'] = model.predict(X)
        
        importance = pd.DataFrame({
            'feature': features,
            'importance': model.feature_importances_
        }).sort_values('importance', ascending=False)
    
    print("\nTop churn risk factors:")
    for _, row in importance.head(5).iterrows():
        print(f"  {row['feature']}: {row['importance']:.3f}")
    
    # Save report
    report_path = os.path.join(REPORTS_DIR, "churn_prediction.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Churn Prediction Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Members analyzed:** {len(df)}\n\n")
        f.write(f"**Method:** Engagement-based scoring + Random Forest (if classes ≥ 2)\n\n")
        
        f.write("## Churn Risk Summary\n\n")
        f.write("| Name | Loans | Searches | Wishlist | Days Idle | Churn Probability | Risk Level |\n")
        f.write("|---|---|---|---|---|---|---|\n")
        for _, row in df.sort_values('churn_probability', ascending=False).iterrows():
            prob = row['churn_probability']
            if prob >= 0.7:
                risk = "🔴 High"
            elif prob >= 0.4:
                risk = "🟡 Medium"
            else:
                risk = "🟢 Low"
            f.write(f"| {row['name']} | {row['total_loans']:.0f} | {row['search_count']:.0f} | {row['wishlist_count']:.0f} | {row['days_since_last_loan']:.0f} | {prob:.1%} | {risk} |\n")
        f.write("\n")
        
        f.write("## Feature Importance\n\n")
        f.write("| Feature | Importance |\n")
        f.write("|---|---|\n")
        for _, row in importance.iterrows():
            f.write(f"| {row['feature']} | {row['importance']:.3f} |\n")
        f.write("\n")
        
        f.write("## Recommendations\n\n")
        f.write("- **High Risk (≥70%)**: Immediate re-engagement email with personalized book suggestions\n")
        f.write("- **Medium Risk (40-69%)**: Targeted newsletter, loyalty rewards reminder\n")
        f.write("- **Low Risk (<40%)**: Maintain engagement with new arrivals notifications\n")
        f.write("\n")
    
    # Save CSV
    csv_path = os.path.join(REPORTS_DIR, "churn_prediction.csv")
    csv_cols = ['name', 'churn_probability', 'prediction', 'engagement_score'] + features
    csv_cols = [c for c in csv_cols if c in df.columns]
    df[csv_cols].to_csv(csv_path, index=False)

    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")
    
    # Print summary
    print(f"\n{'=' * 60}")
    print("CHURN RISK SUMMARY:")
    print(f"{'=' * 60}")
    for _, row in df.sort_values('churn_probability', ascending=False).iterrows():
        risk_emoji = "🔴" if row['churn_probability'] >= 0.7 else "🟡" if row['churn_probability'] >= 0.4 else "🟢"
        print(f"  {risk_emoji} {row['name']}: {row['churn_probability']:.1%} churn probability")
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 4.1 complete!")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
