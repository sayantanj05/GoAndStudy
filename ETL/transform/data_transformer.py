"""
Data transformation module for MongoDB to ClickHouse ETL
"""
import pandas as pd
from typing import List, Dict, Any
from datetime import datetime, date
import numpy as np

class DataTransformer:
    """Transform MongoDB data to ClickHouse schema"""
    
    def __init__(self):
        pass
    
    def transform_dim_user(self, documents: List[Dict]) -> List[Dict]:
        """Transform Member collection to dim_user table"""
        transformed = []
        
        for doc in documents:
            # Calculate real-time age from dateOfBirth if available
            age = self._calculate_real_time_age(doc.get('dateOfBirth'))
            
            transformed_doc = {
                'member_id': str(doc.get('_id', '')),
                'name': doc.get('name', ''),
                'email': doc.get('email', ''),
                'phone': doc.get('phone', ''),
                'role': doc.get('role', ''),
                'membership_type': doc.get('membershipType', ''),
                'preferred_genres': doc.get('preferredGenres', []),
                'total_loans': doc.get('totalLoans'),
                'active_loans': doc.get('activeLoanCount'),
                'date_of_birth': doc.get('dateOfBirth'),
                'age': age,
                'gender': doc.get('gender'),  # Add gender field
                'created_at': doc.get('timeCreated'),
                'updated_at': doc.get('updatedAt', datetime.now())
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_dim_category(self, documents: List[Dict]) -> List[Dict]:
        """Transform book_categories collection to dim_category table"""
        transformed = []
        
        for doc in documents:
            transformed_doc = {
                'category_id': str(doc.get('_id', '')),
                'name': doc.get('name', '')
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_dim_book(self, documents: List[Dict]) -> List[Dict]:
        """Transform Book collection to dim_book table"""
        transformed = []
        
        for doc in documents:
            transformed_doc = {
                'isbn': doc.get('isbn', ''),
                'title': doc.get('title', ''),
                'author_ids': doc.get('authorIds', []),
                'category_ids': doc.get('categoryIds', []),
                'description': doc.get('description', ''),
                'average_rating': doc.get('averageRating'),
                'total_ratings': doc.get('totalRatings'),
                'total_issues': doc.get('totalIssues'),
                'available_copies': doc.get('availableCopies'),
                'created_at': doc.get('createdAt'),
                'updated_at': doc.get('updatedAt', datetime.now())
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_dim_author(self, documents: List[Dict]) -> List[Dict]:
        """Transform Author collection to dim_author table"""
        transformed = []
        
        for doc in documents:
            transformed_doc = {
                'author_id': str(doc.get('id', '')),
                'name': doc.get('name', '')
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_loan(self, documents: List[Dict]) -> List[Dict]:
        """Transform Loan collection to fact_loan table"""
        transformed = []
        
        for doc in documents:
            # Generate time_id from issued_at date
            time_id = self._generate_time_id(doc.get('issuedAt'))
            
            # Calculate real-time overdue days and fine amount
            # Use a copy to avoid modifying the original document
            doc_copy = doc.copy()
            overdue_days, fine_amount = self._calculate_fine_and_overdue(doc_copy)
            
            # Use the potentially updated status from calculation
            final_status = doc_copy.get('status', doc.get('status', ''))
            
            transformed_doc = {
                'loan_id': str(doc.get('_id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': str(doc.get('bookId', '')),
                'isbn': doc.get('bookIsbn', ''),
                'status': final_status,
                'issued_at': doc.get('issuedAt'),
                'due_date': doc.get('dueDate'),
                'returned_at': doc.get('returnedAt'),
                'renewal_count': doc.get('renewalCount'),
                'fine_amount': fine_amount,
                'overdue_days': overdue_days,
                'created_at': doc.get('createdAt'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_ratings(self, documents: List[Dict]) -> List[Dict]:
        """Transform BookReview collection to fact_ratings table"""
        transformed = []
        
        for doc in documents:
            # Get ISBN from bookId by joining with Book collection
            # This will be handled in the ETL orchestrator
            time_id = self._generate_time_id(doc.get('createdAt'))
            
            transformed_doc = {
                'review_id': str(doc.get('id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': str(doc.get('bookId', '')),
                'rating': doc.get('rating'),
                'review_text': doc.get('comment', ''),
                'created_at': doc.get('createdAt'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_wishlist(self, documents: List[Dict]) -> List[Dict]:
        """Transform Wishlist collection to fact_wishlist table"""
        transformed = []
        
        for doc in documents:
            time_id = self._generate_time_id(doc.get('addedAt'))
            
            transformed_doc = {
                'wishlist_id': str(doc.get('id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': str(doc.get('bookId', '')),
                'added_at': doc.get('addedAt'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_reading_session(self, documents: List[Dict]) -> List[Dict]:
        """Transform ReadingSession collection to fact_reading_session table"""
        transformed = []
        
        for doc in documents:
            time_id = self._generate_time_id(doc.get('startedAt'))
            
            transformed_doc = {
                'session_id': str(doc.get('id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': str(doc.get('bookId', '')),
                'session_duration': doc.get('sessionDuration'),
                'pages_read': doc.get('pagesRead'),
                'started_at': doc.get('startedAt'),
                'ended_at': doc.get('endedAt'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_payment(self, documents: List[Dict]) -> List[Dict]:
        """Transform fine_records collection to fact_payment table"""
        transformed = []
        
        for doc in documents:
            # Generate time_id from createdAt or collectedAt
            payment_time = doc.get('collectedAt') or doc.get('createdAt')
            time_id = self._generate_time_id(payment_time)
            
            transformed_doc = {
                'payment_id': str(doc.get('_id', '')),
                'user_id': str(doc.get('memberId', '')),
                'loan_id': str(doc.get('loanId', '')),
                'amount': doc.get('totalAmount'),
                'payment_time': payment_time,
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_events(self, documents: List[Dict]) -> List[Dict]:
        """Transform loan_events collection to fact_events table"""
        transformed = []
        
        for doc in documents:
            # Generate time_id from timestamp
            event_time = doc.get('timestamp')
            time_id = self._generate_time_id(event_time)
            
            transformed_doc = {
                'event_id': str(doc.get('_id', '')),
                'loan_id': str(doc.get('loanId', '')),
                'event_type': doc.get('eventType', ''),
                'timestamp': event_time,
                'triggered_by': doc.get('triggeredBy', ''),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_reservation(self, documents: List[Dict]) -> List[Dict]:
        """Transform Reservation collection to fact_reservation table"""
        transformed = []
        
        for doc in documents:
            time_id = self._generate_time_id(doc.get('reservedAt'))
            
            transformed_doc = {
                'reservation_id': str(doc.get('_id', '')),
                'user_id': str(doc.get('memberId', '')),
                'book_id': str(doc.get('bookId', '')),
                'isbn': doc.get('bookIsbn', ''),  # Will be populated by book mapping
                'requested_at': doc.get('reservedAt'),
                'expiry_date': doc.get('expiresAt'),
                'status': doc.get('status', ''),
                'position_in_queue': doc.get('queuePosition'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_activity_log(self, documents: List[Dict]) -> List[Dict]:
        """Transform ActivityLog collection to fact_activity_log table"""
        transformed = []
        
        for doc in documents:
            time_id = self._generate_time_id(doc.get('timestamp'))
            
            transformed_doc = {
                'activity_id': str(doc.get('id', '')),
                'user_id': str(doc.get('userId', '')),
                'action': doc.get('action', ''),
                'resource': doc.get('resource', ''),
                'timestamp': doc.get('timestamp'),
                'ip_address': doc.get('ipAddress', ''),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def transform_fact_search(self, documents: List[Dict]) -> List[Dict]:
        """Transform search activities from ActivityLog to fact_search table"""
        transformed = []
        
        for doc in documents:
            time_id = self._generate_time_id(doc.get('timestamp'))
            
            # Extract search query from action or resource field
            query = doc.get('action', '')
            if 'search' in query.lower():
                query = doc.get('resource', '')
            
            transformed_doc = {
                'search_id': str(doc.get('id', '')),
                'user_id': str(doc.get('userId', '')),
                'query': query,
                'created_at': doc.get('timestamp'),
                'time_id': time_id
            }
            transformed.append(transformed_doc)
        
        return transformed
    
    def _calculate_real_time_age(self, date_of_birth):
        """Calculate real-time age in years from date of birth"""
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
        
        except Exception:
            return None
    
    def _calculate_fine_and_overdue(self, loan_doc: Dict) -> tuple:
        """Calculate real-time overdue days and fine amount"""
        from datetime import datetime, date
        
        # Default values
        overdue_days = 0
        fine_amount = 0.0
        
        # Get loan details
        due_date = loan_doc.get('dueDate')
        returned_at = loan_doc.get('returnedAt')
        status = loan_doc.get('status', '').upper()
        
        if not due_date:
            return overdue_days, fine_amount
        
        # Parse due date
        try:
            if isinstance(due_date, str):
                due_dt = datetime.fromisoformat(due_date.replace('Z', '+00:00'))
            else:
                due_dt = due_date
        except:
            return overdue_days, fine_amount
        
        # Calculate based on loan status
        if status == 'RETURNED' and returned_at:
            # For returned loans, calculate based on return date
            try:
                if isinstance(returned_at, str):
                    return_dt = datetime.fromisoformat(returned_at.replace('Z', '+00:00'))
                else:
                    return_dt = returned_at
                
                if return_dt > due_dt:
                    overdue_days = (return_dt - due_dt).days
                    fine_amount = overdue_days * 10.0  # $10 per day overdue
            except:
                pass
        elif status != 'RETURNED':
            # For active loans, calculate based on current date
            current_dt = datetime.now()
            if current_dt > due_dt:
                overdue_days = (current_dt - due_dt).days
                fine_amount = overdue_days * 10.0  # $10 per day overdue
                
                # Update status to Overdue if overdue
                if overdue_days > 0:
                    loan_doc['status'] = 'Overdue'
        
        return overdue_days, fine_amount
    
    def _generate_time_id(self, timestamp) -> int:
        """Generate time_id from timestamp (YYYYMMDD format)"""
        if not timestamp:
            return int(datetime.now().strftime('%Y%m%d'))
        
        try:
            if isinstance(timestamp, str):
                dt = datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
            else:
                dt = timestamp
            return int(dt.strftime('%Y%m%d'))
        except:
            return int(datetime.now().strftime('%Y%m%d'))
    
    def transform_all_data(self, extracted_data: Dict[str, List[Dict]], 
                           book_isbn_mapping: Dict[str, str] = None) -> Dict[str, List[Dict]]:
        """Transform all extracted data"""
        transformed_data = {}
        
        # Transform dimension tables
        if 'dim_user' in extracted_data:
            transformed_data['dim_user'] = self.transform_dim_user(extracted_data['dim_user'])
        
        if 'dim_book' in extracted_data:
            transformed_data['dim_book'] = self.transform_dim_book(extracted_data['dim_book'])
        
        if 'dim_author' in extracted_data:
            transformed_data['dim_author'] = self.transform_dim_author(extracted_data['dim_author'])
        
        if 'dim_category' in extracted_data:
            transformed_data['dim_category'] = self.transform_dim_category(extracted_data['dim_category'])
        
        # Transform fact tables
        if 'fact_loan' in extracted_data:
            transformed_data['fact_loan'] = self.transform_fact_loan(extracted_data['fact_loan'])
        
        if 'fact_payment' in extracted_data:
            transformed_data['fact_payment'] = self.transform_fact_payment(extracted_data['fact_payment'])
        
        if 'fact_events' in extracted_data:
            transformed_data['fact_events'] = self.transform_fact_events(extracted_data['fact_events'])
        
        if 'fact_ratings' in extracted_data:
            ratings = self.transform_fact_ratings(extracted_data['fact_ratings'])
            # Fill ISBN from book_isbn_mapping if available
            if book_isbn_mapping:
                for rating in ratings:
                    book_id = rating.get('book_id')  # Original book_id
                    if book_id and book_id in book_isbn_mapping:
                        rating['isbn'] = book_isbn_mapping[book_id]
            transformed_data['fact_ratings'] = ratings
        
        if 'fact_wishlist' in extracted_data:
            transformed_data['fact_wishlist'] = self.transform_fact_wishlist(extracted_data['fact_wishlist'])
        
        if 'fact_reading_session' in extracted_data:
            sessions = self.transform_fact_reading_session(extracted_data['fact_reading_session'])
            # Fill ISBN from book_isbn_mapping if available
            if book_isbn_mapping:
                for session in sessions:
                    book_id = session.get('book_id')  # Original book_id
                    if book_id and book_id in book_isbn_mapping:
                        session['isbn'] = book_isbn_mapping[book_id]
            transformed_data['fact_reading_session'] = sessions
        
        if 'fact_reservation' in extracted_data:
            transformed_data['fact_reservation'] = self.transform_fact_reservation(extracted_data['fact_reservation'])
        
        if 'fact_activity_log' in extracted_data:
            transformed_data['fact_activity_log'] = self.transform_fact_activity_log(extracted_data['fact_activity_log'])
        
        if 'fact_search' in extracted_data:
            transformed_data['fact_search'] = self.transform_fact_search(extracted_data['fact_search'])
        
        return transformed_data
