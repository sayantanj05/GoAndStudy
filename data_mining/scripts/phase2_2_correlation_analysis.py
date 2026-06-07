"""
Phase 2.2: Correlation Analysis
Computes Pearson/Spearman correlations between key metrics.
"""

import os
import clickhouse_connect
import numpy as np
from scipy import stats
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase2_2"
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

def pearson_correlation(x, y, label_x, label_y):
    """Compute Pearson r with significance test."""
    # Remove None/NaN pairs
    pairs = [(a, b) for a, b in zip(x, y) if a is not None and b is not None and not (np.isnan(a) if isinstance(a, float) else False) and not (np.isnan(b) if isinstance(b, float) else False)]
    if len(pairs) < 3:
        return None, None, None, "Insufficient data (n<3)"
    
    x_clean = [p[0] for p in pairs]
    y_clean = [p[1] for p in pairs]
    
    r, p_value = stats.pearsonr(x_clean, y_clean)
    return r, p_value, len(pairs), "Significant" if p_value < 0.05 else "Not significant"

def save_correlation_report(filename, title, description, results):
    filepath = os.path.join(REPORTS_DIR, filename)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(f"# {title}\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Description:** {description}\n\n")
        f.write(f"**Method:** Pearson correlation coefficient (r)\n\n")
        
        f.write("| Hypothesis | r | p-value | n | Interpretation |\n")
        f.write("|---|---|---|---|---|\n")
        for r in results:
            if r['r'] is not None:
                strength = "Strong" if abs(r['r']) > 0.7 else "Moderate" if abs(r['r']) > 0.3 else "Weak"
                direction = "positive" if r['r'] > 0 else "negative"
                f.write(f"| {r['hypothesis']} | {r['r']:.3f} | {r['p']:.4f} | {r['n']} | {strength} {direction} correlation ({r['sig']}) |\n")
            else:
                f.write(f"| {r['hypothesis']} | N/A | N/A | {r['n']} | {r['error']} |\n")
        f.write("\n")
        
        f.write("## Interpretation Guide\n\n")
        f.write("- **|r| > 0.7**: Strong correlation\n")
        f.write("- **|r| > 0.3**: Moderate correlation\n")
        f.write("- **|r| < 0.3**: Weak correlation\n")
        f.write("- **p < 0.05**: Statistically significant\n")
        f.write("\n")
    
    print(f"✅ Saved: {filepath}")
    return filepath

def main():
    print("=" * 60)
    print("PHASE 2.2: CORRELATION ANALYSIS")
    print("=" * 60)
    
    results = []
    
    # C1: Total searches vs Total loans
    print("\n[1/4] Search vs Loan correlation...")
    rows, cols = run_query("""
        SELECT total_searches, total_loans 
        FROM fact_member_analytics
        WHERE total_searches > 0 AND total_loans > 0
    """)
    searches = [r[0] for r in rows]
    loans = [r[1] for r in rows]
    r, p, n, sig = pearson_correlation(searches, loans, "searches", "loans")
    results.append({
        'hypothesis': 'More searches → more loans',
        'r': r, 'p': p, 'n': n, 'sig': sig,
        'error': None if r is not None else "Insufficient data"
    })
    
    # C2: Total overdue vs Total searches
    print("[2/4] Overdue vs Search correlation...")
    rows, cols = run_query("""
        SELECT total_overdue, total_searches 
        FROM fact_member_analytics
        WHERE total_searches > 0
    """)
    overdues = [r[0] for r in rows]
    searches = [r[1] for r in rows]
    r, p, n, sig = pearson_correlation(overdues, searches, "overdues", "searches")
    results.append({
        'hypothesis': 'Overdue users search more',
        'r': r, 'p': p, 'n': n, 'sig': sig,
        'error': None if r is not None else "Insufficient data"
    })
    
    # C3: Reading duration vs Loan frequency
    print("[3/4] Reading duration vs Loan frequency...")
    rows, cols = run_query("""
        SELECT 
            u.user_id,
            SUM(r.session_duration) AS total_reading,
            COUNT(l.loan_id) AS total_loans
        FROM dim_user u
        LEFT JOIN fact_reading_session r ON u.user_id = r.user_id
        LEFT JOIN fact_loan l ON u.user_id = l.user_id
        GROUP BY u.user_id
        HAVING total_reading > 0
    """)
    reading = [r[1] for r in rows]
    loans = [r[2] for r in rows]
    r, p, n, sig = pearson_correlation(reading, loans, "reading", "loans")
    results.append({
        'hypothesis': 'Reading time → loan frequency',
        'r': r, 'p': p, 'n': n, 'sig': sig,
        'error': None if r is not None else "Insufficient data"
    })
    
    # C4: Book avg rating vs borrow count
    print("[4/4] Rating vs Borrow count correlation...")
    rows, cols = run_query("""
        SELECT 
            b.average_rating,
            COUNT(l.loan_id) AS borrow_count
        FROM dim_book b
        LEFT JOIN fact_loan l ON b.book_id = l.book_id
        WHERE b.average_rating > 0
        GROUP BY b.book_id, b.average_rating
    """)
    ratings = [r[0] for r in rows]
    borrows = [r[1] for r in rows]
    r, p, n, sig = pearson_correlation(ratings, borrows, "rating", "borrows")
    results.append({
        'hypothesis': 'Higher rated books borrowed more',
        'r': r, 'p': p, 'n': n, 'sig': sig,
        'error': None if r is not None else "Insufficient data"
    })
    
    save_correlation_report(
        "correlation_matrix.md",
        "Correlation Analysis Matrix",
        "Pearson correlations between key library metrics",
        results
    )
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 2.2 complete! Report saved to: {REPORTS_DIR}")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
