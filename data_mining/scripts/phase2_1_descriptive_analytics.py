"""
Phase 2.1: Descriptive Analytics
Runs SQL queries against ClickHouse and generates markdown reports.
"""

import os
import clickhouse_connect
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase2_1"
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

def save_report(filename: str, title: str, description: str, headers, rows):
    filepath = os.path.join(REPORTS_DIR, filename)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(f"# {title}\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Description:** {description}\n\n")
        f.write(f"**Rows:** {len(rows)}\n\n")
        
        f.write("| " + " | ".join(headers) + " |\n")
        f.write("|" + "|".join(["---" for _ in headers]) + "|\n")
        for row in rows:
            f.write("| " + " | ".join(str(c) for c in row) + " |\n")
        f.write("\n")
    
    print(f"✅ Saved: {filepath} ({len(rows)} rows)")
    return filepath

def analysis_1_top_borrowers():
    rows, cols = run_query("""
        SELECT 
            d.name,
            COUNT(*) AS loans,
            countIf(l.overdue_days > 0) AS overdues,
            SUM(l.fine_amount) AS total_fines,
            ROUND(AVG(l.overdue_days), 1) AS avg_overdue_days
        FROM fact_loan l
        JOIN dim_user d ON l.user_id = d.user_id
        GROUP BY d.name
        ORDER BY loans DESC
    """)
    save_report("01_top_borrowers.md", "Top Borrowers", 
                "Members ranked by total loans, with overdue and fine metrics",
                cols, rows)

def analysis_2_overdue_analysis():
    rows, cols = run_query("""
        SELECT 
            status,
            COUNT(*) AS count,
            ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 1) AS pct,
            ROUND(AVG(overdue_days), 1) AS avg_overdue,
            ROUND(AVG(fine_amount), 2) AS avg_fine
        FROM fact_loan
        GROUP BY status
        ORDER BY count DESC
    """)
    save_report("02_loan_status_breakdown.md", "Loan Status Breakdown",
                "Distribution of loan statuses and associated metrics",
                cols, rows)

def analysis_3_most_borrowed_books():
    rows, cols = run_query("""
        SELECT 
            b.title,
            COUNT(*) AS times_borrowed,
            countIf(l.overdue_days > 0) AS times_overdue,
            ROUND(AVG(l.overdue_days), 1) AS avg_overdue_days,
            b.available_copies
        FROM fact_loan l
        JOIN dim_book b ON l.book_id = b.book_id
        GROUP BY b.title, b.available_copies
        ORDER BY times_borrowed DESC
        LIMIT 15
    """)
    save_report("03_most_borrowed_books.md", "Most Borrowed Books",
                "Books ranked by loan frequency with availability",
                cols, rows)

def analysis_4_genre_popularity():
    rows, cols = run_query("""
        SELECT 
            c.name AS genre,
            COUNT(*) AS loans,
            COUNT(DISTINCT l.user_id) AS unique_borrowers,
            ROUND(AVG(r.rating), 2) AS avg_rating
        FROM fact_loan l
        JOIN dim_book b ON l.book_id = b.book_id
        ARRAY JOIN b.category_ids AS cat_id
        JOIN dim_category c ON cat_id = c.category_id
        LEFT JOIN fact_ratings r ON l.book_id = r.book_id
        GROUP BY c.name
        ORDER BY loans DESC
    """)
    save_report("04_genre_popularity.md", "Genre Popularity",
                "Loan distribution by genre with ratings",
                cols, rows)

def analysis_5_monthly_loan_trends():
    rows, cols = run_query("""
        SELECT 
            d.year AS year,
            d.month_number AS month,
            d.month_name AS month_name,
            COUNT(*) AS loans,
            countIf(l.overdue_days > 0) AS overdues,
            ROUND(SUM(l.fine_amount), 2) AS fines,
            COUNT(DISTINCT l.user_id) AS active_users
        FROM fact_loan l
        JOIN dim_date d ON l.time_id = d.date_key
        GROUP BY d.year, d.month_number, d.month_name
        ORDER BY d.year, d.month_number
    """)
    save_report("05_monthly_loan_trends.md", "Monthly Loan Trends",
                "Loan volume, overdues, and active users by month",
                cols, rows)

def analysis_6_day_of_week_patterns():
    rows, cols = run_query("""
        SELECT 
            d.day_of_week AS dow,
            d.day_name AS day_name,
            COUNT(*) AS loans,
            COUNT(DISTINCT l.user_id) AS unique_users,
            ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 1) AS pct_of_total
        FROM fact_loan l
        JOIN dim_date d ON l.time_id = d.date_key
        GROUP BY d.day_of_week, d.day_name
        ORDER BY d.day_of_week
    """)
    save_report("06_day_of_week_patterns.md", "Day-of-Week Borrowing Patterns",
                "Which days of the week see the most loan activity",
                cols, rows)

def analysis_7_search_to_loan_funnel():
    rows, cols = run_query("""
        SELECT stage, count, unique_users
        FROM (
            SELECT 
                'Searches' AS stage,
                COUNT(*) AS count,
                COUNT(DISTINCT user_id) AS unique_users
            FROM fact_search
            UNION ALL
            SELECT 
                'Wishlists',
                COUNT(*),
                COUNT(DISTINCT user_id)
            FROM fact_wishlist
            UNION ALL
            SELECT 
                'Loans',
                COUNT(*),
                COUNT(DISTINCT user_id)
            FROM fact_loan
            UNION ALL
            SELECT 
                'Ratings',
                COUNT(*),
                COUNT(DISTINCT user_id)
            FROM fact_ratings
        )
        ORDER BY count DESC
    """)
    save_report("07_activity_funnel.md", "Activity Funnel",
                "Search → Wishlist → Loan → Rating conversion stages",
                cols, rows)

def analysis_8_top_search_queries():
    rows, cols = run_query("""
        SELECT 
            query,
            COUNT(*) AS searches,
            COUNT(DISTINCT user_id) AS unique_users
        FROM fact_search
        GROUP BY query
        ORDER BY searches DESC
        LIMIT 20
    """)
    save_report("08_top_searches.md", "Top Search Queries",
                "Most frequent search terms with user diversity",
                cols, rows)

def analysis_9_reading_engagement():
    rows, cols = run_query("""
        SELECT 
            b.title,
            COUNT(*) AS sessions,
            ROUND(AVG(r.session_duration), 1) AS avg_duration_min,
            ROUND(AVG(r.pages_read), 1) AS avg_pages,
            ROUND(AVG(r.pages_read) / NULLIF(AVG(r.session_duration), 0), 2) AS pages_per_min
        FROM fact_reading_session r
        JOIN dim_book b ON r.book_id = b.book_id
        GROUP BY b.title
        ORDER BY sessions DESC
    """)
    save_report("09_reading_engagement.md", "Reading Engagement",
                "Book reading session statistics",
                cols, rows)

def analysis_10_member_analytics():
    rows, cols = run_query("""
        SELECT 
            d.name,
            a.total_loans,
            a.total_overdue,
            ROUND(a.overdue_rate * 100, 1) AS overdue_rate_pct,
            a.total_fines_incurred,
            a.total_fines_paid,
            a.total_searches,
            a.total_ai_calls
        FROM fact_member_analytics a
        JOIN dim_user d ON a.user_id = d.user_id
        ORDER BY a.total_loans DESC
    """)
    save_report("10_member_analytics.md", "Member Analytics Overview",
                "Pre-computed member behavior metrics",
                cols, rows)

def main():
    print("=" * 60)
    print("PHASE 2.1: DESCRIPTIVE ANALYTICS")
    print("=" * 60)
    
    analyses = [
        analysis_1_top_borrowers,
        analysis_2_overdue_analysis,
        analysis_3_most_borrowed_books,
        analysis_4_genre_popularity,
        analysis_5_monthly_loan_trends,
        analysis_6_day_of_week_patterns,
        analysis_7_search_to_loan_funnel,
        analysis_8_top_search_queries,
        analysis_9_reading_engagement,
        analysis_10_member_analytics,
    ]
    
    for i, fn in enumerate(analyses, 1):
        print(f"\n[{i}/{len(analyses)}] Running: {fn.__name__}...")
        try:
            fn()
        except Exception as e:
            print(f"   ⚠️ Error: {e}")
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 2.1 complete! Reports saved to: {REPORTS_DIR}")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
