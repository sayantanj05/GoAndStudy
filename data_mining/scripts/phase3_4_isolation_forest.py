"""
Phase 3.4: Isolation Forest Anomaly Detection
Flags unusual borrowing patterns and suspicious user behavior.
"""

import os
import clickhouse_connect
import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase3_4"
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
    print("PHASE 3.4: ISOLATION FOREST ANOMALY DETECTION")
    print("=" * 60)
    
    # Fetch user behavior metrics
    print("\nFetching user behavior data...")
    rows, cols = run_query("""
        SELECT 
            d.user_id,
            d.name,
            d.email,
            COUNT(DISTINCT l.loan_id) AS total_loans,
            COUNT(DISTINCT CASE WHEN l.status = 'OVERDUE' THEN l.loan_id END) AS overdue_loans,
            SUM(l.fine_amount) AS total_fines,
            COUNT(DISTINCT s.search_id) AS total_searches,
            COUNT(DISTINCT w.wishlist_id) AS wishlist_items,
            COUNT(DISTINCT r.review_id) AS total_reviews,
            AVG(r.rating) AS avg_rating
        FROM dim_user d
        LEFT JOIN fact_loan l ON d.user_id = l.user_id
        LEFT JOIN fact_search s ON d.user_id = s.user_id
        LEFT JOIN fact_wishlist w ON d.user_id = w.user_id
        LEFT JOIN fact_ratings r ON d.user_id = r.user_id
        GROUP BY d.user_id, d.name, d.email
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    print(f"Loaded {len(df)} users")
    print(f"Columns: {list(df.columns)}")
    
    # Feature selection
    features = ['total_loans', 'overdue_loans', 'total_fines', 
                'total_searches', 'wishlist_items', 'total_reviews', 'avg_rating']
    
    # Handle missing values
    for f in features:
        df[f] = pd.to_numeric(df[f], errors='coerce').fillna(0)
    
    X = df[features].values
    
    # Run Isolation Forest
    print("\nTraining Isolation Forest...")
    contamination = min(0.1, max(0.01, 1.0 / len(df)))
    
    model = IsolationForest(
        n_estimators=100,
        contamination=contamination,
        random_state=42
    )
    
    df['anomaly_score'] = model.fit_predict(X)
    df['anomaly'] = df['anomaly_score'] == -1
    
    anomalies = df[df['anomaly'] == True]
    normal = df[df['anomaly'] == False]
    
    print(f"Found {len(anomalies)} anomalous users out of {len(df)}")
    
    # Save report
    report_path = os.path.join(REPORTS_DIR, "anomaly_detection.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Anomaly Detection Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Method:** Isolation Forest (unsupervised)\n\n")
        f.write(f"**Users analyzed:** {len(df)}\n\n")
        f.write(f"**Anomalies detected:** {len(anomalies)} ({len(anomalies)/len(df)*100:.1f}%)\n\n")
        f.write(f"**Contamination rate:** {contamination:.3f}\n\n")
        
        f.write("## Anomalous Users\n\n")
        if len(anomalies) > 0:
            f.write("| Name | Email | Loans | Overdue | Fines | Searches | Wishlist | Reviews | Avg Rating | Reason |\n")
            f.write("|---|---|---|---|---|---|---|---|---|---|\n")
            for _, row in anomalies.iterrows():
                reasons = []
                if row['total_loans'] > normal['total_loans'].quantile(0.95):
                    reasons.append("High loan volume")
                if row['overdue_loans'] > normal['overdue_loans'].quantile(0.95):
                    reasons.append("Many overdue")
                if row['total_fines'] > normal['total_fines'].quantile(0.95):
                    reasons.append("High fines")
                if row['total_searches'] > normal['total_searches'].quantile(0.95):
                    reasons.append("Excessive searches")
                if row['avg_rating'] == 0 and row['total_reviews'] == 0:
                    reasons.append("No engagement")
                
                reason_str = "; ".join(reasons) if reasons else "Unusual pattern"
                
                f.write(f"| {row['name']} | {row['email']} | {row['total_loans']:.0f} | {row['overdue_loans']:.0f} | {row['total_fines']:.2f} | {row['total_searches']:.0f} | {row['wishlist_items']:.0f} | {row['total_reviews']:.0f} | {row['avg_rating']:.1f} | {reason_str} |\n")
        else:
            f.write("No anomalies detected with current threshold.\n")
        
        f.write("\n")
        
        f.write("## Normal User Statistics\n\n")
        f.write("| Metric | Mean | Median | 95th Percentile |\n")
        f.write("|---|---|---|---|\n")
        for feat in features:
            f.write(f"| {feat} | {normal[feat].mean():.2f} | {normal[feat].median():.2f} | {normal[feat].quantile(0.95):.2f} |\n")
        f.write("\n")
        
        f.write("## Business Actions\n\n")
        f.write("1. **Review flagged users**: Check for account sharing or bot behavior\n")
        f.write("2. **High loan volume**: Verify if legitimate power user or suspicious\n")
        f.write("3. **Many overdue**: Send reminders, consider account restrictions\n")
        f.write("4. **Excessive searches**: Possible scraping — monitor IP patterns\n")
        f.write("5. **No engagement**: Inactive accounts — consider re-engagement campaigns\n")
        f.write("\n")
    
    # Save CSV - use all columns
    csv_path = os.path.join(REPORTS_DIR, "anomaly_detection.csv")
    df.to_csv(csv_path, index=False)
    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")
    
    # Print anomalies
    if len(anomalies) > 0:
        print(f"\n{'=' * 60}")
        print("ANOMALOUS USERS:")
        print(f"{'=' * 60}")
        for _, row in anomalies.iterrows():
            print(f"\n⚠️ {row['name']} ({row['email']})")
            print(f"   Loans: {row['total_loans']:.0f}, Overdue: {row['overdue_loans']:.0f}, Fines: {row['total_fines']:.2f}")
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 3.4 complete!")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
