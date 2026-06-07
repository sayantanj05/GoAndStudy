"""
Logging utility for ETL pipeline
"""
import logging
import os
from datetime import datetime
from typing import Optional

class ETLogger:
    """Enhanced logger for ETL operations"""
    
    def __init__(self, name: str = "ETL_Pipeline", log_level: str = "INFO"):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))
        
        # Clear existing handlers
        self.logger.handlers.clear()
        
        # Create formatters
        detailed_formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - %(message)s'
        )
        
        simple_formatter = logging.Formatter(
            '%(asctime)s - %(levelname)s - %(message)s'
        )
        
        # Console handler
        console_handler = logging.StreamHandler()
        console_handler.setFormatter(simple_formatter)
        self.logger.addHandler(console_handler)
        
        # File handler
        log_dir = "logs"
        if not os.path.exists(log_dir):
            os.makedirs(log_dir)
        
        log_file = os.path.join(log_dir, f"etl_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(detailed_formatter)
        self.logger.addHandler(file_handler)
        
        self.logger.info(f"Logger initialized. Log file: {log_file}")
    
    def info(self, message: str, extra: dict = None):
        """Log info message"""
        self.logger.info(message, extra=extra or {})
    
    def error(self, message: str, exception: Exception = None, extra: dict = None):
        """Log error message with exception details"""
        if exception:
            error_msg = f"{message} - Exception: {str(exception)}"
            self.logger.error(error_msg, exc_info=True, extra=extra or {})
        else:
            self.logger.error(message, extra=extra or {})
    
    def warning(self, message: str, extra: dict = None):
        """Log warning message"""
        self.logger.warning(message, extra=extra or {})
    
    def debug(self, message: str, extra: dict = None):
        """Log debug message"""
        self.logger.debug(message, extra=extra or {})
    
    def critical(self, message: str, exception: Exception = None, extra: dict = None):
        """Log critical message"""
        if exception:
            error_msg = f"{message} - Exception: {str(exception)}"
            self.logger.critical(error_msg, exc_info=True, extra=extra or {})
        else:
            self.logger.critical(message, extra=extra or {})
    
    def log_etl_step(self, step_name: str, status: str, details: str = "", records_count: int = None):
        """Log ETL step with standardized format"""
        message = f"ETL Step: {step_name} - Status: {status}"
        if details:
            message += f" - Details: {details}"
        if records_count is not None:
            message += f" - Records: {records_count:,}"
        
        if status.upper() in ["SUCCESS", "COMPLETED"]:
            self.info(message)
        elif status.upper() in ["WARNING", "SKIPPED"]:
            self.warning(message)
        elif status.upper() in ["ERROR", "FAILED"]:
            self.error(message)
        else:
            self.info(message)
    
    def log_performance(self, operation: str, duration_seconds: float, records_processed: int = None):
        """Log performance metrics"""
        message = f"Performance: {operation} took {duration_seconds:.2f} seconds"
        if records_processed:
            rate = records_processed / duration_seconds if duration_seconds > 0 else 0
            message += f" - Rate: {rate:.2f} records/second"
        
        self.info(message)
    
    def log_data_quality(self, table_name: str, total_records: int, 
                        null_counts: dict = None, duplicate_count: int = None):
        """Log data quality metrics"""
        message = f"Data Quality - {table_name}: {total_records:,} total records"
        
        if null_counts:
            null_fields = [f"{field}:{count}" for field, count in null_counts.items() if count > 0]
            if null_fields:
                message += f" - Nulls: {', '.join(null_fields)}"
        
        if duplicate_count is not None:
            message += f" - Duplicates: {duplicate_count:,}"
        
        self.info(message)

# Global logger instance
logger = ETLogger(log_level=os.getenv('LOG_LEVEL', 'INFO'))
