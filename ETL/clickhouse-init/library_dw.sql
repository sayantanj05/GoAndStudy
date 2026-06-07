-- ClickHouse DW Schema for GoAndStudy Library ETL
-- Run manually or auto-executed via /docker-entrypoint-initdb.d on first startup

CREATE DATABASE IF NOT EXISTS library_dw;

-- ============================================================
-- DIMENSION TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.dim_user (
    member_id String,
    name String,
    email String,
    phone String,
    role String,
    membership_type String,
    preferred_genres Array(String),
    total_loans Nullable(Int32),
    active_loans Nullable(Int32),
    date_of_birth Nullable(Date),
    age Nullable(Int32),
    gender Nullable(String),
    created_at Nullable(DateTime),
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY member_id;

CREATE TABLE IF NOT EXISTS library_dw.dim_book (
    isbn String,
    title String,
    author_ids Array(String),
    category_ids Array(String),
    description String,
    average_rating Nullable(Float64),
    total_ratings Nullable(Int32),
    total_issues Nullable(Int32),
    available_copies Nullable(Int32),
    created_at Nullable(DateTime),
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY isbn;

CREATE TABLE IF NOT EXISTS library_dw.dim_author (
    author_id String,
    name String
) ENGINE = ReplacingMergeTree()
ORDER BY author_id;

CREATE TABLE IF NOT EXISTS library_dw.dim_category (
    category_id String,
    name String
) ENGINE = ReplacingMergeTree()
ORDER BY category_id;

CREATE TABLE IF NOT EXISTS library_dw.bridge_book_author (
    isbn String,
    author_id String
) ENGINE = MergeTree()
ORDER BY (isbn, author_id);

-- ============================================================
-- FACT TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.fact_loan (
    loan_id String,
    member_id String,
    isbn String,
    status String,
    issued_at Nullable(DateTime),
    due_date Nullable(DateTime),
    returned_at Nullable(DateTime),
    renewal_count Nullable(Int32),
    fine_amount Nullable(Float64),
    overdue_days Nullable(Int32),
    created_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, loan_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_payment (
    payment_id String,
    member_id String,
    loan_id String,
    amount Nullable(Float64),
    payment_time Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, payment_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_events (
    event_id String,
    loan_id String,
    event_type String,
    timestamp Nullable(DateTime),
    triggered_by String,
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, event_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_ratings (
    review_id String,
    member_id String,
    isbn String,
    rating Nullable(Int32),
    review_text String,
    created_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, review_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_wishlist (
    wishlist_id String,
    member_id String,
    isbn String,
    added_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, wishlist_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_search (
    search_id String,
    member_id String,
    query String,
    created_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, search_id);

-- ============================================================
-- DATE DIMENSION TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.dim_date (
    date_key Int32,
    full_date Date,
    day_of_week UInt8,
    day_name String,
    day_of_month UInt8,
    day_of_year UInt16,
    week_of_year UInt8,
    month_number UInt8,
    month_name String,
    quarter UInt8,
    year UInt16,
    fiscal_quarter UInt8,
    is_weekend UInt8,
    is_holiday UInt8,
    season String
) ENGINE = MergeTree()
ORDER BY date_key;

-- ============================================================
-- ADDITIONAL FACT TABLES (from untapped MongoDB collections)
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.fact_reading_session (
    session_id String,
    member_id String,
    isbn String,
    session_duration Nullable(Int32),
    pages_read Nullable(Int32),
    started_at Nullable(DateTime),
    ended_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, session_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_reservation (
    reservation_id String,
    member_id String,
    isbn String,
    requested_at Nullable(DateTime),
    expiry_date Nullable(DateTime),
    status String,
    position_in_queue Nullable(Int32),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, reservation_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_activity_log (
    activity_id String,
    member_id String,
    action String,
    resource String,
    timestamp Nullable(DateTime),
    ip_address String,
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, activity_id);

-- ============================================================
-- ADDITIONAL DIMENSION & FACT TABLES (Phase 1 Extended)
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.dim_publisher (
    publisher_id String,
    name String,
    location String,
    founded_year Nullable(Int32),
    website String
) ENGINE = ReplacingMergeTree()
ORDER BY publisher_id;

CREATE TABLE IF NOT EXISTS library_dw.dim_member_preferences (
    preference_id String,
    member_id String,
    preferred_authors Array(String),
    preferred_categories Array(String),
    reading_pace_wpm Nullable(Int32),
    preferred_formats Array(String),
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY member_id;


CREATE TABLE IF NOT EXISTS library_dw.fact_book_embedding (
    embedding_id String,
    isbn String,
    embedding Array(Float64),
    model_version String,
    created_at Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, embedding_id);

CREATE TABLE IF NOT EXISTS library_dw.fact_member_analytics (
    analytics_id String,
    member_id String,
    total_loans Nullable(Int32),
    total_returned Nullable(Int32),
    total_overdue Nullable(Int32),
    overdue_rate Nullable(Float64),
    total_fines_incurred Nullable(Float64),
    total_fines_paid Nullable(Float64),
    avg_loan_duration Nullable(Float64),
    total_searches Nullable(Int32),
    total_ai_calls Nullable(Int32),
    ai_conversion_rate Nullable(Float64),
    most_active_month String,
    genre_breakdown String,
    computed_at Nullable(DateTime),
    age Nullable(Int32),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, analytics_id);


CREATE TABLE IF NOT EXISTS library_dw.fact_chatbot_feedback (
    feedback_id String,
    session_id String,
    member_id String,
    rating Nullable(Int32),
    feedback_text String,
    timestamp Nullable(DateTime),
    time_id Int32
) ENGINE = MergeTree()
ORDER BY (time_id, feedback_id);


-- ============================================================
-- RECOMMENDATION SYSTEM TABLES (Phase 1)
-- ============================================================

CREATE TABLE IF NOT EXISTS library_dw.fact_recommendation (
    rec_id String,
    member_id String,
    isbn String,
    section String,
    score Float64,
    model_version String,
    generated_at DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY (member_id, generated_at);
