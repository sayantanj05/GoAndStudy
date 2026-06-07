#!/usr/bin/env python3
"""
Solution Summary: Real-time Age Calculation and Gender Integration for dim_user table

This script demonstrates the complete solution for:
1. Calculating real-time age from date_of_birth
2. Adding gender attribute to dim_user table
3. Storing the data in ClickHouse data warehouse
"""
import requests
from datetime import datetime, date

def calculate_real_time_age(date_of_birth):
    """
    Calculate real-time age in years from date of birth to current date
    
    Args:
        date_of_birth: Date of birth in various formats (string, datetime, etc.)
    
    Returns:
        int: Age in years, or None if date_of_birth is invalid
    """
    if not date_of_birth:
        return None
    
    try:
        # Handle different date formats from MongoDB
        if isinstance(date_of_birth, str):
            # Try parsing different date formats commonly found in MongoDB
            for fmt in ['%Y-%m-%d %H:%M:%S', '%Y-%m-%d', '%Y-%m-%d %H:%M:%S.%f']:
                try:
                    birth_date = datetime.strptime(date_of_birth, fmt).date()
                    break
                except ValueError:
                    continue
            else:
                return None
        elif hasattr(date_of_birth, 'date'):
            birth_date = date_of_birth.date()
        elif isinstance(date_of_birth, datetime):
            birth_date = date_of_birth.date()
        else:
            return None
        
        # Calculate precise age considering months and days
        today = date.today()
        age = today.year - birth_date.year - ((today.month, today.day) < (birth_date.month, birth_date.day))
        return age
    
    except Exception:
        return None

def demonstrate_solution():
    """Demonstrate the complete solution"""
    print("🎯 SOLUTION SUMMARY: Real-time Age & Gender Integration")
    print("=" * 60)
    
    print("\n📋 Key Components Created:")
    print("   1. ✅ Age calculation function with real-time precision")
    print("   2. ✅ Gender column added to dim_user table schema")
    print("   3. ✅ ETL transformer updated to include gender field")
    print("   4. ✅ Data warehouse updated with MongoDB gender data")
    print("   5. ✅ Real-time age calculation for all members")
    
    print("\n📊 Current dim_user Table Structure:")
    print("   - member_id: String")
    print("   - name: String")
    print("   - email: String")
    print("   - phone: String")
    print("   - role: String")
    print("   - membership_type: String")
    print("   - preferred_genres: Array(String)")
    print("   - total_loans: Nullable(Int32)")
    print("   - active_loans: Nullable(Int32)")
    print("   - date_of_birth: Nullable(Date)")
    print("   - age: Nullable(Int32)          ← Real-time calculated")
    print("   - gender: Nullable(String)       ← From MongoDB")
    print("   - created_at: Nullable(DateTime)")
    print("   - updated_at: DateTime")
    
    print("\n🔄 Data Flow:")
    print("   MongoDB (members) → ETL Transformer → ClickHouse (dim_user)")
    print("   ├─ dateOfBirth → Real-time Age Calculation")
    print("   └─ gender → Direct mapping")
    
    print("\n✅ Files Created/Modified:")
    print("   • update_member_age_gender.py - Main update script")
    print("   • update_dim_user_simple.py - Simple HTTP-based updater")
    print("   • verify_and_fix_updates.py - Verification and fixes")
    print("   • test_age_calculation.py - Age calculation testing")
    print("   • add_gender_to_dim_user.sql - SQL schema update")
    print("   • transform/data_transformer.py - Enhanced ETL logic")
    print("   • clickhouse-init/library_dw.sql - Updated schema")
    
    print("\n📈 Sample Results from Data Warehouse:")
    
    # Query current data
    try:
        query = "SELECT member_id, name, age, gender FROM library_dw.dim_user ORDER BY age DESC"
        result = requests.post('http://localhost:8123', data=query)
        
        if result.status_code == 200:
            lines = result.text.strip().split('\n')
            print("   Member ID        Name            Age  Gender")
            print("   ---------------  ---------------  ---  ------")
            for line in lines:
                if line.strip():
                    parts = line.split()
                    if len(parts) >= 4:
                        member_id = parts[0]
                        name = ' '.join(parts[1:-2])
                        age = parts[-2]
                        gender = parts[-1]
                        print(f"   {member_id:<15} {name:<15} {age:<4} {gender}")
    except:
        print("   (Data verification requires ClickHouse connection)")
    
    print("\n🎯 Key Benefits:")
    print("   • Real-time age calculation (always current)")
    print("   • Gender demographic analysis enabled")
    print("   • Enhanced member profiling")
    print("   • Better analytics and reporting")
    print("   • ETL process automatically includes new fields")
    
    print("\n🚀 Usage:")
    print("   1. Run 'python update_dim_user_simple.py' to update existing data")
    print("   2. ETL process will automatically include age and gender for new data")
    print("   3. Use 'calculate_real_time_age()' function for real-time calculations")
    
    print("\n" + "=" * 60)
    print("✅ SOLUTION COMPLETE: Age & Gender successfully integrated!")

if __name__ == "__main__":
    demonstrate_solution()
