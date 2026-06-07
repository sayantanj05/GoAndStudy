# MongoDB Atlas to ClickHouse ETL Pipeline

Complete ETL pipeline to migrate data from MongoDB Atlas to ClickHouse data warehouse for the GoAndStudy library system.

## 🏗️ Architecture

```
MongoDB Atlas → ETL Pipeline → ClickHouse DW
     ↓              ↓              ↓
  Source Data    Extract       Data Warehouse
  (29 Collections) Transform    (Optimized Schema)
                Load
```

## 📋 Prerequisites

1. **MongoDB Atlas Access**
   - IP address whitelisted (152.59.167.83/32 added, 152.59.167.165/32 added)
   - Connection string with credentials
   - Access to GoAndStudy database

2. **ClickHouse Server**
   - Running ClickHouse instance
   - Network access from ETL machine
   - Appropriate permissions

3. **Python Environment**
   - Python 3.8+
   - Required packages (see requirements.txt)

## 🚀 Quick Start

### 1. Setup Environment

```bash
# Copy environment configuration
cp .env.example .env

# Edit .env with your actual credentials
# MongoDB Atlas connection string
# ClickHouse connection details
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Run ETL Pipeline

```bash
python main.py
```

## 📁 Project Structure

```
ETL/
├── main.py                    # Main ETL orchestrator
├── requirements.txt           # Python dependencies
├── .env.example              # Environment configuration template
├── config/
│   └── database.py           # Database connection management
├── extract/
│   └── mongo_extractor.py    # MongoDB data extraction
├── transform/
│   └── data_transformer.py   # Data transformation logic
├── load/
│   └── clickhouse_loader.py  # ClickHouse data loading
├── utils/
│   └── logger.py             # Enhanced logging utility
├── clickhouse-init/
│   └── library_dw.sql       # ClickHouse schema definition
└── logs/                     # ETL execution logs
```

## 🔄 ETL Process

### 1. Extract
- Connects to MongoDB Atlas
- Extracts data from all 29 collections
- Handles ObjectId to string conversion
- Processes data in configurable batches

### 2. Transform
- Maps MongoDB schema to ClickHouse DW schema
- Converts data types and formats
- Handles null values and missing fields
- Creates time_id for fact tables
- Generates book_id to ISBN mappings

### 3. Load
- Creates ClickHouse database and tables
- Loads dimension tables first (for FK constraints)
- Loads fact tables with proper relationships
- Creates date dimension table
- Provides data verification

## 📊 Data Mapping

### MongoDB Collections → ClickHouse Tables

| MongoDB Collection | ClickHouse Table | Notes |
|-------------------|------------------|-------|
| Member | dim_user | Member dimension |
| Book | dim_book | Book dimension (ISBN as key) |
| Author | dim_author | Author dimension |
| BookCategory | dim_category | Category dimension |
| Publisher | dim_publisher | Publisher dimension |
| MemberPreferences | dim_member_preferences | Member preferences |
| Loan | fact_loan | Loan fact table |
| BookReview | fact_ratings | Ratings fact table |
| Wishlist | fact_wishlist | Wishlist fact table |
| ReadingSession | fact_reading_session | Reading sessions |
| Reservation | fact_reservation | Book reservations |
| ActivityLog | fact_activity_log | User activities |
| ActivityLog (search) | fact_search | Search activities |
| FineRecord | fact_payment | Payment/fine records |
| BookEmbedding | fact_book_embedding | Book embeddings |
| MemberAnalytics | fact_member_analytics | Member analytics |
| ChatbotFeedback | fact_chatbot_feedback | Chatbot feedback |
| RecommendationFeedback | fact_recommendation | Recommendations |

## ⚙️ Configuration

### Environment Variables

```bash
# MongoDB Atlas
MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/GoAndStudy?retryWrites=true&w=majority
MONGO_DB_NAME=GoAndStudy

# ClickHouse
CLICKHOUSE_HOST=localhost
CLICKHOUSE_PORT=8123
CLICKHOUSE_USER=default
CLICKHOUSE_PASSWORD=
CLICKHOUSE_DB=library_dw

# ETL Settings
BATCH_SIZE=1000
LOG_LEVEL=INFO
ENABLE_DEBUG=false
```

## 📈 Features

### Data Quality
- ObjectId to string conversion
- Null value handling
- Data type validation
- Duplicate detection
- Record count verification

### Performance
- Batch processing for large datasets
- Configurable batch sizes
- Memory-efficient processing
- Progress tracking

### Error Handling
- Comprehensive exception handling
- Detailed error logging
- Transaction safety
- Rollback capabilities

### Monitoring
- Detailed logging to console and files
- Performance metrics
- Data quality reports
- Execution summaries

## 🔧 Customization

### Adding New Collections

1. Update `collections_map` in `mongo_extractor.py`
2. Add transformation logic in `data_transformer.py`
3. Update schema in `library_dw.sql`
4. Add loading logic in `clickhouse_loader.py`

### Modifying Transformations

Edit the `transform_*` methods in `data_transformer.py` to customize:
- Field mappings
- Data type conversions
- Business logic
- Validation rules

## 🐛 Troubleshooting

### Common Issues

1. **Connection Failed**
   - Check IP whitelist in MongoDB Atlas
   - Verify connection string format
   - Ensure ClickHouse is accessible

2. **Schema Errors**
   - Verify ClickHouse table definitions
   - Check data type compatibility
   - Ensure proper column ordering

3. **Memory Issues**
   - Reduce BATCH_SIZE in configuration
   - Monitor system resources
   - Enable batch processing

### Debug Mode

Set `ENABLE_DEBUG=true` in `.env` for detailed debugging information.

## 📝 Logging

Logs are written to:
- Console output (real-time)
- `logs/etl_YYYYMMDD_HHMMSS.log` (detailed logs)

Log levels: DEBUG, INFO, WARNING, ERROR, CRITICAL

## 🚀 Production Deployment

### Docker Deployment

```bash
# Build ETL image
docker build -t goandstudy-etl .

# Run ETL container
docker run --env-file .env goandstudy-etl
```

### Scheduled Execution

Use cron or task scheduler for regular ETL runs:

```bash
# Daily at 2 AM
0 2 * * * cd /path/to/ETL && python main.py
```

## 📊 Performance Metrics

Typical performance for 100K records:
- Extraction: ~30 seconds
- Transformation: ~45 seconds  
- Loading: ~60 seconds
- Total: ~2-3 minutes

## 🔐 Security

- Store credentials in environment variables
- Use SSL connections for MongoDB
- Implement proper access controls
- Regular credential rotation

## 📞 Support

For issues and questions:
1. Check logs in `logs/` directory
2. Verify configuration in `.env`
3. Review error messages carefully
4. Check MongoDB Atlas IP whitelist
5. Verify ClickHouse connectivity
