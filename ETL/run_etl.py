#!/usr/bin/env python3
"""
ETL Pipeline Runner
Simple script to run the ETL pipeline with proper error handling
"""
import os
import sys
import argparse
from datetime import datetime

def setup_environment():
    """Setup environment and check prerequisites"""
    # Check if .env file exists
    if not os.path.exists('.env'):
        print("❌ Error: .env file not found!")
        print("Please copy .env.example to .env and configure your settings:")
        print("  cp .env.example .env")
        print("  # Edit .env with your MongoDB Atlas and ClickHouse credentials")
        return False
    
    # Check required directories
    required_dirs = ['logs', 'config', 'extract', 'transform', 'load', 'utils']
    for dir_name in required_dirs:
        if not os.path.exists(dir_name):
            print(f"❌ Error: Required directory '{dir_name}' not found!")
            return False
    
    # Check if ClickHouse schema file exists
    schema_file = os.path.join('clickhouse-init', 'library_dw.sql')
    if not os.path.exists(schema_file):
        print(f"❌ Error: Schema file '{schema_file}' not found!")
        return False
    
    print("✅ Environment setup verified")
    return True

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Run MongoDB Atlas to ClickHouse ETL Pipeline')
    parser.add_argument('--verbose', '-v', action='store_true', 
                       help='Enable verbose logging')
    parser.add_argument('--dry-run', action='store_true',
                       help='Run in dry-run mode (extract and transform only)')
    
    args = parser.parse_args()
    
    print("🚀 MongoDB Atlas to ClickHouse ETL Pipeline")
    print("=" * 50)
    
    # Setup environment
    if not setup_environment():
        sys.exit(1)
    
    # Set log level based on verbose flag
    if args.verbose:
        os.environ['LOG_LEVEL'] = 'DEBUG'
        print("🔍 Verbose mode enabled")
    
    try:
        # Import and run ETL pipeline
        from main import ETLPipeline
        
        pipeline = ETLPipeline()
        
        if args.dry_run:
            print("🧪 Dry-run mode: Extract and transform only")
            # TODO: Implement dry-run functionality
            print("⚠️  Dry-run mode not yet implemented")
            return
        
        # Run full ETL
        success = pipeline.run_full_etl()
        
        if success:
            print("\n🎉 ETL Pipeline completed successfully!")
            print(f"📊 Check the logs/ directory for detailed execution logs")
            print(f"🔍 Connect to ClickHouse to verify the migrated data")
        else:
            print("\n💥 ETL Pipeline failed!")
            print(f"📝 Check the logs/ directory for error details")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n⚠️  ETL Pipeline interrupted by user")
        sys.exit(1)
    except ImportError as e:
        print(f"❌ Import error: {e}")
        print("Make sure all required packages are installed:")
        print("  pip install -r requirements.txt")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
