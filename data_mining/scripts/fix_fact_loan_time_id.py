"""
One-time fix for fact_loan time_id = 0 rows.
Extracts timestamp from loan_id (MongoDB ObjectId) and updates time_id.
"""

import clickhouse_connect
from bson import ObjectId
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def get_ch_client():
    return clickhouse_connect.get_client(
        host='localhost', port=8123,
        username='default', password='',
        database='library_dw'
    )

def extract_time_id_from_oid(oid_str: str) -> int:
    """Extract YYYYMMDD from MongoDB ObjectId string."""
    try:
        oid = ObjectId(oid_str)
        return int(oid.generation_time.strftime('%Y%m%d'))
    except Exception:
        return 0

def fix_fact_loan_time_ids():
    ch = get_ch_client()
    try:
        # Get all loan_ids with time_id = 0
        result = ch.query("""
            SELECT loan_id 
            FROM fact_loan 
            WHERE time_id = 0
        """)
        
        loan_ids = [row[0] for row in result.result_rows]
        logger.info(f"Found {len(loan_ids)} fact_loan rows with time_id = 0")
        
        if not loan_ids:
            logger.info("No fixes needed.")
            return
        
        # Build update data: (time_id, loan_id) pairs
        updates = []
        for loan_id in loan_ids:
            time_id = extract_time_id_from_oid(loan_id)
            if time_id > 0:
                updates.append((time_id, loan_id))
        
        logger.info(f"Prepared {len(updates)} valid updates")
        
        # Batch update via ALTER TABLE ... UPDATE
        # ClickHouse doesn't support row-by-row UPDATE well, so we use a different approach:
        # Create a temporary table with corrections, then JOIN-update
        
        if updates:
            # Create temporary correction table
            ch.command("""
                CREATE OR REPLACE TABLE tmp_time_id_fix (
                    loan_id String,
                    new_time_id Int32
                ) ENGINE = Memory
            """)
            
            # Insert corrections
            ch.insert('tmp_time_id_fix', updates)
            
            # Update fact_loan via JOIN
            ch.command("""
                ALTER TABLE fact_loan
                UPDATE time_id = new_time_id
                WHERE loan_id IN (SELECT loan_id FROM tmp_time_id_fix)
                  AND time_id = 0
            """)
            
            # Verify
            remaining = ch.query("SELECT count() FROM fact_loan WHERE time_id = 0").result_rows[0][0]
            logger.info(f"Remaining zero time_ids after fix: {remaining}")
            
            # Cleanup
            ch.command("DROP TABLE IF EXISTS tmp_time_id_fix")
            
    finally:
        ch.close()

if __name__ == "__main__":
    fix_fact_loan_time_ids()
