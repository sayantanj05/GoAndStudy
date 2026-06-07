#!/usr/bin/env python3
"""
Test script for age calculation functionality
"""
from datetime import datetime, date

def calculate_real_time_age(date_of_birth):
    """Calculate age in years from date of birth to current date"""
    if not date_of_birth:
        return None
    
    try:
        # Handle different date formats
        if isinstance(date_of_birth, str):
            # Try parsing different date formats
            for fmt in ['%Y-%m-%d %H:%M:%S', '%Y-%m-%d', '%Y-%m-%d %H:%M:%S.%f']:
                try:
                    birth_date = datetime.strptime(date_of_birth, fmt).date()
                    break
                except ValueError:
                    continue
            else:
                print(f"⚠️ Could not parse date: {date_of_birth}")
                return None
        elif hasattr(date_of_birth, 'date'):
            birth_date = date_of_birth.date()
        elif isinstance(date_of_birth, datetime):
            birth_date = date_of_birth.date()
        else:
            return None
        
        today = date.today()
        age = today.year - birth_date.year - ((today.month, today.day) < (birth_date.month, birth_date.day))
        return age
    
    except Exception as e:
        print(f"❌ Error calculating age for {date_of_birth}: {e}")
        return None

def test_age_calculation():
    """Test age calculation with sample dates"""
    print("🧪 Testing age calculation...")
    
    test_cases = [
        ('2005-09-18 18:30:00', 'Sayantan Jana'),
        ('1992-12-13 18:30:00', 'Arijit Das'),
        ('2026-05-03 18:30:00', 'Rahul Sharma (infant)'),
        ('1990-01-01', 'Test person 1990'),
        (None, 'No date'),
        ('invalid-date', 'Invalid date')
    ]
    
    today = date.today()
    print(f"📅 Today's date: {today}")
    print()
    
    for dob, name in test_cases:
        age = calculate_real_time_age(dob)
        print(f"👤 {name}")
        print(f"   DOB: {dob}")
        print(f"   Age: {age} years")
        print()

if __name__ == "__main__":
    test_age_calculation()
