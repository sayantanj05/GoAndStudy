"""
Phase 4.3: Fine Amount Estimation
Predicts fine amount for overdue loans using Linear Regression.
Handles small/empty datasets gracefully.
"""

import os
import clickhouse_connect
import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, r2_score
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase4_3"
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
    print("PHASE 4.3: FINE AMOUNT ESTIMATION")
    print("=" * 60)
    
    print("\nFetching overdue loan data...")
    rows, cols = run_query("""
        SELECT 
            l.loan_id,
            l.user_id,
            d.name,
            l.isbn,
            b.title,
            l.fine_amount,
            l.overdue_days,
            l.renewal_count,
            COUNT(DISTINCT prev_loan.loan_id) AS member_total_loans,
            COUNT(DISTINCT CASE WHEN prev_loan.status = 'OVERDUE' THEN prev_loan.loan_id END) AS member_overdue_count,
            SUM(prev_loan.fine_amount) AS member_total_fines
        FROM fact_loan l
        JOIN dim_user d ON l.user_id = d.user_id
        LEFT JOIN dim_book b ON l.book_id = b.book_id
        LEFT JOIN fact_loan prev_loan ON l.user_id = prev_loan.user_id AND prev_loan.issued_at < l.issued_at
        WHERE l.status = 'OVERDUE' OR l.fine_amount > 0 OR l.overdue_days > 0
        GROUP BY l.loan_id, l.user_id, d.name, l.isbn, b.title,
                 l.fine_amount, l.overdue_days, l.renewal_count
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    # ClickHouse may retain table prefixes like l.column_name
    df.columns = [c.split('.', 1)[-1] if c.startswith('l.') else c for c in df.columns]
    print(f"Loaded {len(df)} loans with potential fines")
    print(f"Available columns: {list(df.columns)}")
    
    # Early exit if no data
    if len(df) == 0:
        print("\n⚠️ No overdue or fined loans found in dataset")
        generate_report_only(df)
        return
    
    # Safe numeric conversion
    for col in ['fine_amount', 'overdue_days', 'renewal_count', 
                'member_total_loans', 'member_overdue_count', 'member_total_fines']:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
    
    # Safe derived columns
    if 'overdue_days' in df.columns:
        df['days_overdue'] = df['overdue_days']
    else:
        df['days_overdue'] = 0
    
    if 'member_overdue_count' in df.columns:
        df['has_previous_overdue'] = (df['member_overdue_count'] > 0).astype(int)
    else:
        df['has_previous_overdue'] = 0
    
    print(f"\nFine distribution:")
    print(f"  Loans with fines: {len(df[df['fine_amount'] > 0])}")
    print(f"  Loans without fines: {len(df[df['fine_amount'] == 0])}")
    
    features = ['days_overdue', 'renewal_count', 'member_total_loans', 
                'member_overdue_count', 'member_total_fines', 'has_previous_overdue']
    # Only use features that exist
    features = [f for f in features if f in df.columns]
    
    df_train = df[df['fine_amount'] > 0].copy()
    
    if len(df_train) < 3:
        print("\n⚠️ Insufficient fine data for regression model")
        generate_report_only(df)
        return
    
    X = df_train[features]
    y = df_train['fine_amount']
    
    print(f"\nTraining Linear Regression on {len(df_train)} fined loans...")
    model = LinearRegression()
    model.fit(X, y)
    
    df['expected_fine'] = model.predict(df[features])
    df['expected_fine'] = df['expected_fine'].clip(lower=0)
    
    y_pred = model.predict(X)
    mae = mean_absolute_error(y, y_pred)
    r2 = r2_score(y, y_pred) if len(y) > 1 else 0
    
    print(f"\nModel performance:")
    print(f"  MAE: {mae:.2f}")
    print(f"  R²: {r2:.3f}")
    
    importance = pd.DataFrame({
        'feature': features,
        'coefficient': model.coef_
    }).sort_values('coefficient', key=abs, ascending=False)
    
    print("\nFine estimation factors:")
    for _, row in importance.iterrows():
        direction = "increases" if row['coefficient'] > 0 else "decreases"
        print(f"  {row['feature']}: {direction} fine (coef={row['coefficient']:.3f})")
    
    generate_report(df, df_train, importance, model, mae, r2)

def generate_report(df, df_train, importance, model, mae, r2):
    report_path = os.path.join(REPORTS_DIR, "fine_estimation.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Fine Amount Estimation Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Model:** Linear Regression\n\n")
        f.write(f"**Loans analyzed:** {len(df)}\n\n")
        f.write(f"**Loans with fines (training set):** {len(df_train)}\n\n")
        
        f.write("## Model Performance\n\n")
        f.write(f"| Metric | Value |\n")
        f.write(f"|---|---|\n")
        f.write(f"| Mean Absolute Error | {mae:.2f} |\n")
        f.write(f"| R² Score | {r2:.3f} |\n")
        f.write(f"| Intercept | {model.intercept_:.2f} |\n")
        f.write("\n")
        
        f.write("## Feature Importance\n\n")
        f.write("| Feature | Coefficient | Effect |\n")
        f.write("|---|---|---|\n")
        for _, row in importance.iterrows():
            effect = "Increases fine" if row['coefficient'] > 0 else "Decreases fine"
            f.write(f"| {row['feature']} | {row['coefficient']:.3f} | {effect} |\n")
        f.write("\n")
        
        f.write("## Fine Estimation by Loan\n\n")
        f.write("| Member | Book | Actual Fine | Expected Fine | Days Overdue | Diff |\n")
        f.write("|---|---|---|---|---|---|\n")
        for _, row in df.iterrows():
            diff = row['fine_amount'] - row['expected_fine']
            title = str(row['title'])[:25] if pd.notna(row.get('title')) else 'N/A'
            f.write(f"| {row['name']} | {title}... | {row['fine_amount']:.2f} | {row['expected_fine']:.2f} | {row['days_overdue']:.0f} | {diff:+.2f} |\n")
        f.write("\n")
        
        f.write("## Business Use Cases\n\n")
        f.write("1. **Transparent Expectations**: Show members expected fine at checkout\n")
        f.write("2. **Early Warning**: Flag high-risk loans before they become overdue\n")
        f.write("3. **Dynamic Due Dates**: Adjust due dates based on member history\n")
        f.write("4. **Fine Appeals**: Use model to validate or contest fine amounts\n")
        f.write("\n")
    
    csv_path = os.path.join(REPORTS_DIR, "fine_estimation.csv")
    csv_cols = [c for c in ['loan_id', 'user_id', 'name', 'title', 'fine_amount', 'expected_fine', 'days_overdue'] if c in df.columns]
    df[csv_cols].to_csv(csv_path, index=False)
    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")

def generate_report_only(df):
    report_path = os.path.join(REPORTS_DIR, "fine_estimation.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Fine Amount Estimation Report\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("⚠️ **Cannot train regression model with current data**\n\n")
        
        if len(df) == 0:
            f.write("Reason: No overdue or fined loans found in the dataset\n\n")
        else:
            f.write("Reason: Insufficient loans with actual fine amounts\n\n")
            f.write(f"Loans analyzed: {len(df)}\n\n")
            if 'fine_amount' in df.columns:
                f.write(f"Loans with actual fines: {len(df[df['fine_amount'] > 0])}\n\n")
        
        f.write("## Data Requirements\n\n")
        f.write("For accurate fine estimation, need:\n")
        f.write("- 10+ loans with actual fine amounts\n")
        f.write("- Varied overdue durations\n")
        f.write("- Members with multiple overdue history\n")
        f.write("\n")
    
    print(f"\n✅ Saved: {report_path}")

if __name__ == "__main__":
    main()
