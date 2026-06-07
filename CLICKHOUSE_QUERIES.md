# ClickHouse Terminal SQL Query Guide

ClickHouse is running in Docker (`AirFlow/docker-compose.yaml`) on ports:
- **8123** — HTTP interface (for `curl`, Python, browser)
- **9000** — Native TCP (for `clickhouse-client`)

Database: `library_dw`

---

## Method 1: Docker `exec` (Recommended)

Open an interactive ClickHouse client inside the container:

```powershell
docker exec -it airflow-clickhouse-1 clickhouse-client --database library_dw
```

> Replace `airflow-clickhouse-1` with your actual container name. Find it with:
> ```powershell
> docker ps --filter "ancestor=clickhouse/clickhouse-server" --format "{{.Names}}"
> ```

Then run any SQL query, e.g.:

```sql
SHOW TABLES;
SELECT * FROM dim_user LIMIT 5;
```

---

## Method 2: One-shot Docker queries

```powershell
docker exec airflow-clickhouse-1 clickhouse-client --database library_dw --query "SELECT count() FROM dim_user"
```

---

## Method 3: `curl` HTTP API

```powershell
curl -s "http://localhost:8123/?database=library_dw&query=SELECT+count()+FROM+dim_user"
```

Multi-line queries via POST:

```powershell
curl -s -X POST "http://localhost:8123/?database=library_dw" --data "
SELECT name, email FROM dim_user LIMIT 5
"
```

Pretty output with `FORMAT Pretty`:

```powershell
curl -s -X POST "http://localhost:8123/?database=library_dw" --data "
SELECT * FROM dim_book LIMIT 5
FORMAT Pretty
"
```

---

## Method 4: Python (`clickhouse-connect`)

```python
import clickhouse_connect
client = clickhouse_connect.get_client(host='localhost', port=8123, database='library_dw')
result = client.query("SELECT * FROM dim_user LIMIT 5")
print(result.result_rows)
```

---

## Useful SQL Queries

### 1. List all tables
```sql
SHOW TABLES FROM library_dw;
```

### 2. Row counts for every table
```sql
SELECT 'dim_user' AS table, count() AS rows FROM dim_user
UNION ALL SELECT 'dim_book', count() FROM dim_book
UNION ALL SELECT 'dim_author', count() FROM dim_author
UNION ALL SELECT 'dim_category', count() FROM dim_category
UNION ALL SELECT 'dim_date', count() FROM dim_date
UNION ALL SELECT 'dim_publisher', count() FROM dim_publisher
UNION ALL SELECT 'dim_member_preferences', count() FROM dim_member_preferences
UNION ALL SELECT 'bridge_book_author', count() FROM bridge_book_author
UNION ALL SELECT 'fact_loan', count() FROM fact_loan
UNION ALL SELECT 'fact_payment', count() FROM fact_payment
UNION ALL SELECT 'fact_events', count() FROM fact_events
UNION ALL SELECT 'fact_ratings', count() FROM fact_ratings
UNION ALL SELECT 'fact_wishlist', count() FROM fact_wishlist
UNION ALL SELECT 'fact_search', count() FROM fact_search
UNION ALL SELECT 'fact_reading_session', count() FROM fact_reading_session
UNION ALL SELECT 'fact_reservation', count() FROM fact_reservation
UNION ALL SELECT 'fact_activity_log', count() FROM fact_activity_log
UNION ALL SELECT 'fact_book_embedding', count() FROM fact_book_embedding
UNION ALL SELECT 'fact_member_analytics', count() FROM fact_member_analytics
UNION ALL SELECT 'fact_chatbot_feedback', count() FROM fact_chatbot_feedback;
```

### 3. Sample users
```sql
SELECT user_id, name, email, role, membership_type
FROM dim_user
LIMIT 5;
```

### 4. Top searched terms
```sql
SELECT query, count() AS searches
FROM fact_search
GROUP BY query
ORDER BY searches DESC
LIMIT 10;
```

### 5. Loans per user
```sql
SELECT
    d.name,
    count() AS loan_count
FROM fact_loan f
JOIN dim_user d ON f.user_id = d.user_id
GROUP BY d.name
ORDER BY loan_count DESC
LIMIT 10;
```

### 6. Books with highest average rating
```sql
SELECT
    book_id,
    title,
    average_rating,
    total_ratings
FROM dim_book
WHERE average_rating > 0
ORDER BY average_rating DESC
LIMIT 10;
```

### 7. Loan status distribution
```sql
SELECT status, count() AS count
FROM fact_loan
GROUP BY status;
```

### 8. Recent search activity
```sql
SELECT user_id, query, created_at
FROM fact_search
ORDER BY created_at DESC
LIMIT 10;
```

### 9. Wishlist per user
```sql
SELECT
    d.name,
    count() AS wishlist_count
FROM fact_wishlist w
JOIN dim_user d ON w.user_id = d.user_id
GROUP BY d.name
ORDER BY wishlist_count DESC
LIMIT 10;
```

### 10. Time-series loan volume
```sql
SELECT
    time_id,
    count() AS loans
FROM fact_loan
GROUP BY time_id
ORDER BY time_id
LIMIT 20;
```

### 11. Describe a table's schema
```sql
DESCRIBE dim_user;
DESCRIBE fact_loan;
```

### 12. Check for NULL time_id values
```sql
SELECT table, count() AS null_time_ids
FROM (
    SELECT 'fact_loan' AS table, * FROM fact_loan WHERE time_id = 0
    UNION ALL
    SELECT 'fact_search' AS table, * FROM fact_search WHERE time_id = 0
)
GROUP BY table;
```

### 13. Data freshness (latest record per table)
```sql
SELECT 'dim_user' AS table, max(created_at) AS latest FROM dim_user
UNION ALL
SELECT 'dim_book', max(created_at) FROM dim_book
UNION ALL
SELECT 'fact_loan', max(created_at) FROM fact_loan
UNION ALL
SELECT 'fact_search', max(created_at) FROM fact_search;
```

---

## Phase 1 Data Mining Queries

### 14. Check dim_date population
```sql
SELECT
    min(full_date) AS earliest,
    max(full_date) AS latest,
    count() AS total_days
FROM dim_date;
```

### 15. Loans by day of week (using dim_date)
```sql
SELECT
    d.day_name,
    count() AS loans
FROM fact_loan f
JOIN dim_date d ON f.time_id = d.date_key
GROUP BY d.day_name, d.day_of_week
ORDER BY d.day_of_week;
```

### 16. Monthly loan trends (using dim_date)
```sql
SELECT
    d.year,
    d.month_name,
    count() AS loans
FROM fact_loan f
JOIN dim_date d ON f.time_id = d.date_key
GROUP BY d.year, d.month_name, d.month_number
ORDER BY d.year, d.month_number;
```

### 17. Seasonal borrowing patterns
```sql
SELECT
    d.season,
    count() AS loans
FROM fact_loan f
JOIN dim_date d ON f.time_id = d.date_key
GROUP BY d.season
ORDER BY loans DESC;
```

### 18. Reading session stats per user
```sql
SELECT
    u.name,
    count() AS sessions,
    avg(session_duration) AS avg_duration_min,
    sum(pages_read) AS total_pages
FROM fact_reading_session r
JOIN dim_user u ON r.user_id = u.user_id
GROUP BY u.name
ORDER BY sessions DESC
LIMIT 10;
```

### 19. Reservation queue status
```sql
SELECT
    status,
    count() AS reservations,
    avg(position_in_queue) AS avg_queue_position
FROM fact_reservation
GROUP BY status;
```

### 20. Most active users (activity log)
```sql
SELECT
    u.name,
    count() AS actions,
    uniqExact(resource) AS unique_resources
FROM fact_activity_log a
JOIN dim_user u ON a.user_id = u.user_id
GROUP BY u.name
ORDER BY actions DESC
LIMIT 10;
```

### 21. RFM Analysis (Recency, Frequency, Monetary)
```sql
SELECT
    u.user_id,
    u.name,
    dateDiff('day', max(l.issued_at), today()) AS recency_days,
    count() AS frequency,
    sum(l.fine_amount) AS monetary
FROM fact_loan l
JOIN dim_user u ON l.user_id = u.user_id
GROUP BY u.user_id, u.name
ORDER BY frequency DESC, recency_days ASC
LIMIT 20;
```

### 22. Publisher distribution
```sql
SELECT
    p.name,
    p.location,
    count() AS books_published
FROM dim_book b
ARRAY JOIN b.author_ids AS author_id
JOIN dim_publisher p ON p.publisher_id = b.book_id
GROUP BY p.name, p.location
ORDER BY books_published DESC
LIMIT 10;
```

### 23. Member reading preferences
```sql
SELECT
    u.name,
    mp.preferred_authors,
    mp.preferred_categories,
    mp.reading_pace_wpm
FROM dim_member_preferences mp
JOIN dim_user u ON mp.user_id = u.user_id
LIMIT 10;
```

### 24. Chatbot feedback sentiment
```sql
SELECT
    rating,
    count() AS feedback_count,
    avg(rating) AS avg_rating
FROM fact_chatbot_feedback
GROUP BY rating
ORDER BY rating;
```

### 25. Member analytics overview
```sql
SELECT
    u.name,
    ma.books_read,
    ma.total_fines,
    ma.active_loans,
    ma.avg_rating_given
FROM fact_member_analytics ma
JOIN dim_user u ON ma.user_id = u.user_id
ORDER BY ma.books_read DESC
LIMIT 10;
```

---

## Quick PowerShell Copy-Paste

**Get row counts for all tables:**
```powershell
docker exec airflow-clickhouse-1 clickhouse-client --database library_dw --query "
SELECT 'dim_user' AS table, count() AS rows FROM dim_user
UNION ALL SELECT 'dim_book', count() FROM dim_book
UNION ALL SELECT 'dim_author', count() FROM dim_author
UNION ALL SELECT 'dim_date', count() FROM dim_date
UNION ALL SELECT 'dim_publisher', count() FROM dim_publisher
UNION ALL SELECT 'dim_member_preferences', count() FROM dim_member_preferences
UNION ALL SELECT 'fact_loan', count() FROM fact_loan
UNION ALL SELECT 'fact_search', count() FROM fact_search
UNION ALL SELECT 'fact_ratings', count() FROM fact_ratings
UNION ALL SELECT 'fact_wishlist', count() FROM fact_wishlist
UNION ALL SELECT 'fact_reading_session', count() FROM fact_reading_session
UNION ALL SELECT 'fact_reservation', count() FROM fact_reservation
UNION ALL SELECT 'fact_activity_log', count() FROM fact_activity_log
UNION ALL SELECT 'fact_book_embedding', count() FROM fact_book_embedding
UNION ALL SELECT 'fact_member_analytics', count() FROM fact_member_analytics
UNION ALL SELECT 'fact_chatbot_feedback', count() FROM fact_chatbot_feedback
FORMAT Pretty
"
```

**Top 5 searched queries:**
```powershell
docker exec airflow-clickhouse-1 clickhouse-client --database library_dw --query "
SELECT query, count() AS searches
FROM fact_search
GROUP BY query
ORDER BY searches DESC
LIMIT 5
FORMAT Pretty
"
