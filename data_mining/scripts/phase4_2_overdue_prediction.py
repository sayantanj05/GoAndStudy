"""
Phase 4.2: Overdue Prediction
Predicts if a loan will become overdue based on member history and book attributes.
"""

import os
import clickhouse_connect
import pandas as pd
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, roc_auc_score
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase4_2"
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
    print("PHASE 4.2: OVERDUE PREDICTION")
    print("=" * 60)
    
    # Fetch loan data with member history and book attributes
    print("\nFetching loan data...")
    rows, cols = run_query("""
        SELECT 
            l.loan_id,
            l.user_id,
            d.name,
            b.title,
            l.isbn,
            l.status,
            l.issued_at,
            l.due_date,
            l.fine_amount,
            l.overdue_days,
            COUNT(DISTINCT prev.loan_id) AS member_prev_loans,
            COUNT(DISTINCT CASE WHEN prev.status = 'OVERDUE' THEN prev.loan_id END) AS member_prev_overdue,
            AVG(prev_r.rating) AS member_avg_rating,
            b.total_issues AS book_total_issues,
            b.average_rating AS book_avg_rating
        FROM fact_loan l
        JOIN dim_user d ON l.user_id = d.user_id
        JOIN dim_book b ON l.book_id = b.book_id
        LEFT JOIN fact_loan prev ON l.user_id = prev.user_id AND prev.issued_at < l.issued_at
        LEFT JOIN fact_ratings prev_r ON l.user_id = prev_r.user_id
        GROUP BY l.loan_id, l.user_id, d.name, b.title, l.isbn, l.status, 
                 l.issued_at, l.due_date, l.fine_amount, l.overdue_days,
                 b.total_issues, b.average_rating
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    # ClickHouse may retain table prefixes like l.column_name
    df.columns = [c.split('.', 1)[-1] if c.startswith('l.') else c for c in df.columns]
    print(f"Loaded {len(df)} loans")
    print(f"Available columns: {list(df.columns)}")

    
    # Handle missing values — only process columns that actually exist
    numeric_cols = ['fine_amount', 'overdue_days', 'member_prev_loans', 
                    'member_prev_overdue', 'member_avg_rating',
                    'book_total_issues', 'book_avg_rating']
    for col in numeric_cols:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
    
    # Target: 1 if OVERDUE, else 0
    if 'status' not in df.columns:
        print(f"\n⚠️ 'status' column missing from ClickHouse result")
        print(f"   Cannot compute overdue labels without status")
        generate_report_only(df, df)
        return
    
    df['is_overdue'] = (df['status'] == 'OVERDUE').astype(int)

    
    print(f"\nOverdue distribution:")
    print(f"  On-time: {len(df[df['is_overdue'] == 0])}")
    print(f"  Overdue: {len(df[df['is_overdue'] == 1])}")
    
    # Features — only include columns that exist in the DataFrame
    all_features = ['member_prev_loans', 'member_prev_overdue', 'member_avg_rating',
                    'book_total_issues', 'book_avg_rating', 'overdue_days', 'fine_amount']
    features = [f for f in all_features if f in df.columns]
    
    if not features:
        print("\n⚠️ No usable features found in dataset")
        generate_report_only(df, df)
        return
    
    # Remove rows where all features are 0 (insufficient history)
    df_model = df[df[features].sum(axis=1) > 0].copy()

    
    if len(df_model) < 5:
        print("\n⚠️ Insufficient data for reliable overdue prediction")
        print("   Generating report with available analysis...")
        generate_report_only(df, df_model)
        return
    
    X = df_model[features]
    y = df_model['is_overdue']
    
    unique_classes = np.unique(y)
    if len(unique_classes) == 1:
        print(f"\n⚠️ Only one class found in data (all loans are {'OVERDUE' if unique_classes[0] == 1 else 'on-time'})")
        print("   Cannot train binary classifier without both classes")
        generate_report_only(df, df_model)
        return
    
    # Train model
    print("\nTraining Logistic Regression...")
    model = LogisticRegression(random_state=42, max_iter=1000)
    model.fit(X, y)

    
    # Predictions on all loans
    df['overdue_probability'] = model.predict_proba(df[features])[:, 1]
    df['prediction'] = model.predict(df[features])
    
    # Feature importance (coefficients)
    importance = pd.DataFrame({
        'feature': features,
        'coefficient': model.coef_[0]
    })
    importance['abs_coef'] = importance['coefficient'].abs()
    importance = importance.sort_values('abs_coef', ascending=False)
    
    print("\nTop overdue risk factors:")
    for _, row in importance.head(5).iterrows():
        direction = "increases" if row['coefficient'] > 0 else "decreases"
        print(f"  {row['feature']}: {direction} risk (coef={row['coefficient']:.3f})")
    
    # Save report
    generate_report(df, df_model, importance, model)

def generate_report(df, df_model, importance, model):
    report_path = os.path.join(REPORTS_DIR, "overdue_prediction.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Overdue Prediction Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Model:** Logistic Regression\n\n")
        f.write(f"**Loans analyzed:** {len(df)}\n\n")
        f.write(f"**Loans with sufficient history:** {len(df_model)}\n\n")
        
        f.write("## Overdue Risk by Loan\n\n")
        f.write("| Member | Book | Prev Overdue | Overdue Probability | Risk Level |\n")
        f.write("|---|---|---|---|---|\n")
        for _, row in df.sort_values('overdue_probability', ascending=False).iterrows():
            prob = row['overdue_probability']
            if prob >= 0.6:
                risk = "🔴 High"
            elif prob >= 0.3:
                risk = "🟡 Medium"
            else:
                risk = "🟢 Low"
            title = str(row['title'])[:30] if pd.notna(row.get('title')) else 'N/A'
            f.write(f"| {row['name']} | {title}... | {row.get('member_prev_overdue', 0):.0f} | {prob:.1%} | {risk} |\n")

        f.write("\n")
        
        f.write("## Feature Importance\n\n")
        f.write("| Feature | Coefficient | Effect |\n")
        f.write("|---|---|---|\n")
        for _, row in importance.iterrows():
            effect = "Increases risk" if row['coefficient'] > 0 else "Decreases risk"
            f.write(f"| {row['feature']} | {row['coefficient']:.3f} | {effect} |\n")
        f.write("\n")
        
        f.write("## Recommendations\n\n")
        f.write("- **High Risk**: Send due date reminder 3 days early\n")
        f.write("- **Medium Risk**: Standard due date reminder\n")
        f.write("- **Low Risk**: No special action needed\n")
        f.write("- Members with previous overdue history need closer monitoring\n")
        f.write("\n")
    
    # Save CSV
    csv_path = os.path.join(REPORTS_DIR, "overdue_prediction.csv")
    base_cols = ['loan_id', 'user_id', 'name', 'title', 'status']
    extra_cols = [c for c in ['overdue_probability', 'prediction', 'member_prev_loans', 'member_prev_overdue', 'book_total_issues'] if c in df.columns]
    csv_cols = [c for c in (base_cols + extra_cols) if c in df.columns]
    df[csv_cols].to_csv(csv_path, index=False)

    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")

def generate_report_only(df, df_model):
    """Fallback when insufficient data for model training"""
    report_path = os.path.join(REPORTS_DIR, "overdue_prediction.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Overdue Prediction Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("⚠️ **Cannot train reliable model with current data**\n\n")
        if 'status' in df.columns and 'OVERDUE' not in df['status'].values:
            f.write("Reason: All loans are on-time — no overdue examples to learn from\n\n")
        else:
            f.write("Reason: Insufficient historical data diversity\n\n")

        f.write(f"Loans analyzed: {len(df)}\n\n")
        
        if 'status' in df.columns:
            f.write("## Current Loan Status Distribution\n\n")
            status_counts = df['status'].value_counts()
            for status, count in status_counts.items():
                f.write(f"- {status}: {count}\n")
            f.write("\n")
        else:
            f.write("## Available Columns\n\n")
            f.write(f"Columns in dataset: {', '.join(df.columns)}\n\n")
        
        f.write("## Data Requirements\n\n")
        f.write("For accurate overdue prediction, need:\n")
        f.write("- 20+ loans with varied member history\n")
        f.write("- Members with both on-time and overdue loans\n")
        f.write("- Book attribute diversity\n")
        f.write("\n")
    
    print(f"\n✅ Saved: {report_path}")


if __name__ == "__main__":
    main()
