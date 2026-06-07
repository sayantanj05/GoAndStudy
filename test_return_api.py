#!/usr/bin/env python3
"""
Test script to simulate the return book API call
"""

import requests
import json

def test_active_loans_api():
    """Test the active loans API endpoint"""
    base_url = "http://localhost:8080"
    target_isbn = "978-8197156847"
    
    print("🧪 Testing Return Book API")
    print("=" * 40)
    
    try:
        # Test 1: Get active loans without authentication
        print(f"\n1. Testing GET /api/v1/staff/loans/active?q={target_isbn}")
        response = requests.get(
            f"{base_url}/api/v1/staff/loans/active",
            params={"q": target_isbn},
            timeout=10
        )
        
        print(f"   Status Code: {response.status_code}")
        print(f"   Response: {response.text[:200]}...")
        
        if response.status_code == 200:
            data = response.json()
            loans = data.get('data', {}).get('loans', [])
            print(f"   Found {len(loans)} loans")
            
            # Check for ISBN match
            matching_loans = []
            for loan in loans:
                loan_isbn = loan.get('bookIsbn', '')
                if loan_isbn.lower() == target_isbn.lower():
                    matching_loans.append(loan)
            
            print(f"   Matching loans: {len(matching_loans)}")
            
            if matching_loans:
                print(f"   ✅ Found matching loan:")
                loan = matching_loans[0]
                print(f"      Book: {loan.get('bookTitle', 'N/A')}")
                print(f"      Member: {loan.get('memberName', 'N/A')}")
                print(f"      Loan ID: {loan.get('id', 'N/A')}")
                return True
            else:
                print(f"   ❌ No matching loan found")
                print(f"   Available ISBNs:")
                for loan in loans[:5]:
                    print(f"      - {loan.get('bookIsbn', 'N/A')} ({loan.get('bookTitle', 'N/A')})")
                return False
        else:
            print(f"   ❌ API call failed")
            return False
            
    except Exception as e:
        print(f"❌ Test failed: {e}")
        return False

def test_isbn_format():
    """Test different ISBN formats"""
    base_url = "http://localhost:8080"
    
    print(f"\n2. Testing different ISBN formats...")
    
    isbns_to_test = [
        "978-8197156847",
        "9788197156847", 
        "8197156847"
    ]
    
    for isbn in isbns_to_test:
        try:
            response = requests.get(
                f"{base_url}/api/v1/staff/loans/active",
                params={"q": isbn},
                timeout=5
            )
            
            print(f"   ISBN {isbn}: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                loans = data.get('data', {}).get('loans', [])
                matching = [l for l in loans if l.get('bookIsbn', '').replace('-', '').lower() == isbn.replace('-', '').lower()]
                print(f"      Matching loans: {len(matching)}")
            
        except Exception as e:
            print(f"   ISBN {isbn}: Error - {e}")

def main():
    """Main test function"""
    success = test_active_loans_api()
    test_isbn_format()
    
    if success:
        print(f"\n🎯 CONCLUSION:")
        print(f"   The API is working correctly.")
        print(f"   The error is likely in the frontend:")
        print(f"   1. Authentication token missing/invalid")
        print(f"   2. Frontend error handling issue")
        print(f"   3. Network connectivity problem")
    else:
        print(f"\n🎯 CONCLUSION:")
        print(f"   The API call is failing.")
        print(f"   This could explain the frontend error.")

if __name__ == "__main__":
    main()
