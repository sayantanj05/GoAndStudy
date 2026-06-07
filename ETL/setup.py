#!/usr/bin/env python3
"""
ETL Pipeline Setup Script
Initial setup for MongoDB Atlas to ClickHouse ETL
"""
import os
import sys
import shutil

def create_directories():
    """Create necessary directories"""
    directories = [
        'logs',
        'config', 
        'extract',
        'transform',
        'load',
        'utils',
        'clickhouse-init'
    ]
    
    for directory in directories:
        if not os.path.exists(directory):
            os.makedirs(directory)
            print(f"✅ Created directory: {directory}")
        else:
            print(f"📁 Directory exists: {directory}")

def setup_environment():
    """Setup environment configuration"""
    if not os.path.exists('.env'):
        if os.path.exists('.env.example'):
            shutil.copy('.env.example', '.env')
            print("✅ Created .env from .env.example")
            print("⚠️  Please edit .env with your actual credentials")
        else:
            print("❌ .env.example not found!")
            return False
    else:
        print("📁 .env file already exists")
    
    return True

def verify_requirements():
    """Verify requirements.txt exists"""
    if os.path.exists('requirements.txt'):
        print("✅ requirements.txt found")
        return True
    else:
        print("❌ requirements.txt not found!")
        return False

def main():
    """Main setup function"""
    print("🚀 Setting up MongoDB Atlas to ClickHouse ETL Pipeline")
    print("=" * 60)
    
    # Create directories
    print("\n📁 Creating directories...")
    create_directories()
    
    # Setup environment
    print("\n⚙️  Setting up environment...")
    if not setup_environment():
        sys.exit(1)
    
    # Verify requirements
    print("\n📋 Verifying requirements...")
    if not verify_requirements():
        sys.exit(1)
    
    print("\n✅ Setup completed successfully!")
    print("\n📝 Next steps:")
    print("1. Edit .env with your MongoDB Atlas and ClickHouse credentials")
    print("2. Install dependencies: pip install -r requirements.txt")
    print("3. Run the ETL pipeline: python run_etl.py")
    print("\n📚 For more information, see README.md")

if __name__ == "__main__":
    main()
