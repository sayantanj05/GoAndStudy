"""
Predictive model wrappers for Phase 5 Deployment.
Wraps Phase 4 Random Forest, Logistic Regression, and Linear Regression
into reusable production classes with ClickHouse integration.
"""

import os
import pickle
import logging
import numpy as np
import pandas as pd
from typing import Optional, List, Dict, Any
from datetime import datetime
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression, LinearRegression
from sklearn.preprocessing import MinMaxScaler

from scripts.data_loader import get_client, query_to_df

logger = logging.getLogger(__name__)
MODELS_DIR = os.path.join(os.path.dirname(__file__), '..', 'models', 'predictions')
os.makedirs(MODELS_DIR, exist_ok=True)


class ChurnPredictor:
    """Random Forest churn prediction with engagement score features."""
    
    MODEL_PATH = os.path.join(MODELS_DIR, 'churn_model.pkl')
    SCALER_PATH = os.path.join(MODELS_DIR, 'churn_scaler.pkl')
    
    def __init__(self):
        self.model: Optional[RandomForestClassifier] = None
        self.scaler: Optional[MinMaxScaler] = None
        self.feature_names = [
            'total_loans', 'active_loans', 'days_since_last_loan',
            'search_count', 'wishlist_count', 'avg_rating'
        ]
        self._load_or_train()
    
    def _load_or_train(self):
        """Load saved model or train fresh on demand."""
        if os.path.exists(self.MODEL_PATH):
            logger.info("Loading saved churn model...")
            with open(self.MODEL_PATH, 'rb') as f:
                self.model = pickle.load(f)
            if os.path.exists(self.SCALER_PATH):
                with open(self.SCALER_PATH, 'rb') as f:
                    self.scaler = pickle.load(f)
        else:
            logger.info("No saved churn model found, will train on first prediction.")
    
    def _load_member_data(self, client) -> pd.DataFrame:
        """Load member engagement data from ClickHouse."""
        query = """
            SELECT 
                d.user_id,
                d.name,
                d.membership_type,
                COALESCE(d.total_loans, 0) AS total_loans,
                COALESCE(d.active_loans, 0) AS active_loans,
                COUNT(DISTINCT l.loan_id) AS actual_loans,
                COUNT(DISTINCT s.search_id) AS search_count,
                COUNT(DISTINCT w.wishlist_id) AS wishlist_count,
                COALESCE(AVG(r.rating), 0) AS avg_rating,
                MAX(l.issued_at) AS last_loan_date
            FROM dim_user d
            LEFT JOIN fact_loan l ON d.user_id = l.user_id
            LEFT JOIN fact_search s ON d.user_id = s.user_id
            LEFT JOIN fact_wishlist w ON d.user_id = w.user_id
            LEFT JOIN fact_ratings r ON d.user_id = r.user_id
            GROUP BY d.user_id, d.name, d.membership_type,
                     d.total_loans, d.active_loans
        """
        df = query_to_df(client, query)
        df.columns = [c.split('.', 1)[-1] if c.startswith('l.') or c.startswith('d.') or c.startswith('s.') or c.startswith('w.') or c.startswith('r.') else c for c in df.columns]
        return df
    
    def _engineer_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Create engagement score and derived features."""
        now = pd.Timestamp.now()
        
        # Days since last loan
        if 'last_loan_date' in df.columns:
            df['last_loan_date'] = pd.to_datetime(df['last_loan_date'], errors='coerce')
            df['days_since_last_loan'] = (now - df['last_loan_date']).dt.days.fillna(30).astype(int)
        else:
            df['days_since_last_loan'] = 30
        
        # Ensure numeric
        for col in ['total_loans', 'active_loans', 'search_count', 'wishlist_count', 'avg_rating']:
            if col not in df.columns:
                df[col] = 0
            df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
        
        # Use actual_loans if total_loans is zero
        if 'actual_loans' in df.columns:
            df['actual_loans'] = pd.to_numeric(df['actual_loans'], errors='coerce').fillna(0)
            df['total_loans'] = np.where(df['total_loans'] == 0, df['actual_loans'], df['total_loans'])
        
        # Engagement score
        scaler = MinMaxScaler()
        features_to_scale = df[['total_loans', 'days_since_last_loan', 'search_count', 'wishlist_count', 'avg_rating']].copy()
        features_to_scale['days_since_last_loan'] = features_to_scale['days_since_last_loan'].clip(upper=30)
        scaled = scaler.fit_transform(features_to_scale)
        df['engagement_score'] = scaled.mean(axis=1)
        
        # Binary churn label (bottom 30% by engagement = at-risk)
        threshold = df['engagement_score'].median()
        df['churn_label'] = (df['engagement_score'] < threshold).astype(int)
        
        return df
    
    def train(self, client=None) -> bool:
        """Train the churn prediction model."""
        try:
            if client is None:
                client = get_client()
            df = self._load_member_data(client)
            if len(df) < 5:
                logger.warning(f"Only {len(df)} members, not enough to train churn model")
                return False
            
            df = self._engineer_features(df)
            
            X = df[self.feature_names].fillna(0)
            y = df['churn_label']
            
            if y.nunique() <= 1:
                logger.warning("Only one class in churn data, cannot train")
                return False
            
            self.scaler = MinMaxScaler()
            X_scaled = self.scaler.fit_transform(X)
            
            self.model = RandomForestClassifier(n_estimators=50, random_state=42, max_depth=5)
            self.model.fit(X_scaled, y)
            
            # Save
            with open(self.MODEL_PATH, 'wb') as f:
                pickle.dump(self.model, f)
            with open(self.SCALER_PATH, 'wb') as f:
                pickle.dump(self.scaler, f)
            
            logger.info(f"Churn model trained on {len(df)} members")
            return True
            
        except Exception as e:
            logger.error(f"Churn training failed: {e}")
            return False
    
    def predict(self, user_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Predict churn risk for all members or a specific user."""
        try:
            client = get_client()
            df = self._load_member_data(client)
            if len(df) == 0:
                return []
            
            df = self._engineer_features(df)
            
            # Train if not loaded
            if self.model is None or self.scaler is None:
                success = self.train(client)
                if not success:
                    # Fallback to heuristic
                    return self._heuristic_predict(df)
            
            X = df[self.feature_names].fillna(0)
            X_scaled = self.scaler.transform(X)
            
            probabilities = self.model.predict_proba(X_scaled)[:, 1]
            df['churn_probability'] = 1 - probabilities  # invert: low engagement = high churn
            
            if user_id:
                df = df[df['user_id'] == user_id]
            
            results = []
            for _, row in df.iterrows():
                prob = float(row['churn_probability'])
                results.append({
                    'user_id': str(row['user_id']),
                    'user_name': str(row.get('name', 'Unknown')),
                    'membership_type': str(row.get('membership_type', '')),
                    'churn_probability': round(prob, 2),
                    'risk_level': 'HIGH' if prob > 0.7 else 'MEDIUM' if prob > 0.4 else 'LOW',
                    'engagement_score': round(float(row['engagement_score']), 3),
                    'total_loans': int(row['total_loans']),
                    'days_since_last_loan': int(row['days_since_last_loan']),
                    'top_features': self._get_top_features()
                })
            
            return sorted(results, key=lambda x: x['churn_probability'], reverse=True)
            
        except Exception as e:
            logger.error(f"Churn prediction failed: {e}")
            return []
    
    def _heuristic_predict(self, df: pd.DataFrame) -> List[Dict[str, Any]]:
        """Fallback heuristic when model can't train."""
        results = []
        for _, row in df.iterrows():
            loans = int(row.get('total_loans', 0))
            days = int(row.get('days_since_last_loan', 30))
            prob = min(1.0, (30 - min(days, 30)) / 30 * 0.5 + (10 - min(loans, 10)) / 10 * 0.5)
            results.append({
                'user_id': str(row['user_id']),
                'user_name': str(row.get('name', 'Unknown')),
                'membership_type': str(row.get('membership_type', '')),
                'churn_probability': round(prob, 2),
                'risk_level': 'HIGH' if prob > 0.7 else 'MEDIUM' if prob > 0.4 else 'LOW',
                'engagement_score': 0.0,
                'total_loans': loans,
                'days_since_last_loan': days,
                'top_features': ['total_loans', 'days_since_last_loan'],
                'note': 'heuristic_fallback'
            })
        return sorted(results, key=lambda x: x['churn_probability'], reverse=True)
    
    def _get_top_features(self) -> List[Dict[str, float]]:
        """Return feature importances if model exists."""
        if self.model is None:
            return []
        importances = self.model.feature_importances_
        return [
            {'feature': name, 'importance': round(float(imp), 3)}
            for name, imp in sorted(zip(self.feature_names, importances), key=lambda x: x[1], reverse=True)
        ]


class OverduePredictor:
    """Logistic Regression overdue prediction."""
    
    MODEL_PATH = os.path.join(MODELS_DIR, 'overdue_model.pkl')
    
    def __init__(self):
        self.model: Optional[LogisticRegression] = None
        self.feature_names = [
            'member_prev_loans', 'member_prev_overdue', 'member_avg_rating',
            'book_total_issues', 'book_avg_rating', 'overdue_days', 'fine_amount'
        ]
        self._load_or_train()
    
    def _load_or_train(self):
        if os.path.exists(self.MODEL_PATH):
            logger.info("Loading saved overdue model...")
            with open(self.MODEL_PATH, 'rb') as f:
                self.model = pickle.load(f)
    
    def _load_loan_data(self, client) -> pd.DataFrame:
        query = """
            SELECT 
                l.loan_id, l.user_id, d.name, b.title, l.isbn, l.status,
                l.issued_at, l.due_date, l.fine_amount, l.overdue_days,
                COUNT(DISTINCT prev.loan_id) AS member_prev_loans,
                COUNT(DISTINCT CASE WHEN prev.status = 'OVERDUE' THEN prev.loan_id END) AS member_prev_overdue,
                AVG(prev_r.rating) AS member_avg_rating,
                b.total_issues AS book_total_issues,
                b.average_rating AS book_avg_rating
            FROM fact_loan l
            JOIN dim_user d ON l.user_id = d.user_id
            JOIN dim_book b ON l.book_id = b.book_id
            LEFT JOIN fact_loan prev ON l.user_id = prev.user_id AND prev.issued_at < l.issued_at
            LEFT JOIN fact_ratings prev_r ON l.user_id = prev_r.user_id
            GROUP BY l.loan_id, l.user_id, d.name, b.title, l.isbn, l.status,
                     l.issued_at, l.due_date, l.fine_amount, l.overdue_days,
                     b.total_issues, b.average_rating
        """
        df = query_to_df(client, query)
        df.columns = [c.split('.', 1)[-1] if c.startswith('l.') or c.startswith('d.') or c.startswith('b.') or c.startswith('prev.') or c.startswith('prev_r.') else c for c in df.columns]
        return df
    
    def train(self, client=None) -> bool:
        try:
            if client is None:
                client = get_client()
            df = self._load_loan_data(client)
            if len(df) < 5:
                logger.warning(f"Only {len(df)} loans, not enough to train overdue model")
                return False
            
            for col in ['fine_amount', 'overdue_days', 'member_prev_loans', 'member_prev_overdue', 'member_avg_rating', 'book_total_issues', 'book_avg_rating']:
                if col in df.columns:
                    df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
            
            if 'status' not in df.columns:
                logger.warning("No status column in loan data")
                return False
            
            df['is_overdue'] = (df['status'] == 'OVERDUE').astype(int)
            
            if df['is_overdue'].nunique() <= 1:
                logger.warning(f"Only one class in overdue data: {df['is_overdue'].unique()}")
                return False
            
            features = [f for f in self.feature_names if f in df.columns]
            X = df[features].fillna(0)
            y = df['is_overdue']
            
            self.model = LogisticRegression(random_state=42, max_iter=1000)
            self.model.fit(X, y)
            
            with open(self.MODEL_PATH, 'wb') as f:
                pickle.dump(self.model, f)
            
            logger.info(f"Overdue model trained on {len(df)} loans")
            return True
            
        except Exception as e:
            logger.error(f"Overdue training failed: {e}")
            return False
    
    def predict(self, loan_id: Optional[str] = None) -> List[Dict[str, Any]]:
        try:
            client = get_client()
            df = self._load_loan_data(client)
            if len(df) == 0:
                return []
            
            for col in ['fine_amount', 'overdue_days', 'member_prev_loans', 'member_prev_overdue', 'member_avg_rating', 'book_total_issues', 'book_avg_rating']:
                if col in df.columns:
                    df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
            
            if 'status' not in df.columns:
                return []
            
            # Train if needed
            if self.model is None:
                success = self.train(client)
                if not success:
                    return self._heuristic_predict(df)
            
            features = [f for f in self.feature_names if f in df.columns]
            X = df[features].fillna(0)
            probabilities = self.model.predict_proba(X)[:, 1]
            df['overdue_probability'] = probabilities
            
            if loan_id:
                df = df[df['loan_id'] == loan_id]
            
            results = []
            for _, row in df.iterrows():
                prob = float(row['overdue_probability'])
                results.append({
                    'loan_id': str(row['loan_id']),
                    'user_id': str(row['user_id']),
                    'member_name': str(row.get('name', 'Unknown')),
                    'book_title': str(row.get('title', 'Unknown'))[:40],
                    'status': str(row.get('status', '')),
                    'overdue_probability': round(prob, 2),
                    'risk_level': 'HIGH' if prob >= 0.6 else 'MEDIUM' if prob >= 0.3 else 'LOW',
                    'member_prev_overdue': int(row.get('member_prev_overdue', 0)),
                    'book_total_issues': int(row.get('book_total_issues', 0))
                })
            
            return sorted(results, key=lambda x: x['overdue_probability'], reverse=True)
            
        except Exception as e:
            logger.error(f"Overdue prediction failed: {e}")
            return []
    
    def _heuristic_predict(self, df: pd.DataFrame) -> List[Dict[str, Any]]:
        results = []
        for _, row in df.iterrows():
            prev_overdue = int(row.get('member_prev_overdue', 0))
            prob = min(0.95, prev_overdue * 0.3 + 0.1)
            results.append({
                'loan_id': str(row['loan_id']),
                'user_id': str(row['user_id']),
                'member_name': str(row.get('name', 'Unknown')),
                'book_title': str(row.get('title', 'Unknown'))[:40],
                'status': str(row.get('status', '')),
                'overdue_probability': round(prob, 2),
                'risk_level': 'HIGH' if prob >= 0.6 else 'MEDIUM' if prob >= 0.3 else 'LOW',
                'member_prev_overdue': prev_overdue,
                'book_total_issues': int(row.get('book_total_issues', 0)),
                'note': 'heuristic_fallback'
            })
        return sorted(results, key=lambda x: x['overdue_probability'], reverse=True)


class FineEstimator:
    """Linear Regression fine amount estimation."""
    
    MODEL_PATH = os.path.join(MODELS_DIR, 'fine_model.pkl')
    
    def __init__(self):
        self.model: Optional[LinearRegression] = None
        self.feature_names = [
            'days_overdue', 'renewal_count', 'member_total_loans',
            'member_overdue_count', 'member_total_fines', 'has_previous_overdue'
        ]
        self._load_or_train()
    
    def _load_or_train(self):
        if os.path.exists(self.MODEL_PATH):
            logger.info("Loading saved fine model...")
            with open(self.MODEL_PATH, 'rb') as f:
                self.model = pickle.load(f)
    
    def _load_fine_data(self, client) -> pd.DataFrame:
        query = """
            SELECT 
                l.loan_id, l.user_id, d.name, l.isbn, b.title,
                l.fine_amount, l.overdue_days, l.renewal_count,
                COUNT(DISTINCT prev_loan.loan_id) AS member_total_loans,
                COUNT(DISTINCT CASE WHEN prev_loan.status = 'OVERDUE' THEN prev_loan.loan_id END) AS member_overdue_count,
                SUM(prev_loan.fine_amount) AS member_total_fines
            FROM fact_loan l
            JOIN dim_user d ON l.user_id = d.user_id
            LEFT JOIN dim_book b ON l.book_id = b.book_id
            LEFT JOIN fact_loan prev_loan ON l.user_id = prev_loan.user_id AND prev_loan.issued_at < l.issued_at
            WHERE l.status = 'OVERDUE' OR l.fine_amount > 0 OR l.overdue_days > 0
            GROUP BY l.loan_id, l.user_id, d.name, l.isbn, b.title,
                     l.fine_amount, l.overdue_days, l.renewal_count
        """
        df = query_to_df(client, query)
        df.columns = [c.split('.', 1)[-1] if c.startswith('l.') or c.startswith('d.') or c.startswith('b.') or c.startswith('prev_loan.') else c for c in df.columns]
        return df
    
    def train(self, client=None) -> bool:
        try:
            if client is None:
                client = get_client()
            df = self._load_fine_data(client)
            if len(df) < 5:
                logger.warning(f"Only {len(df)} fined loans, not enough to train fine model")
                return False
            
            for col in ['fine_amount', 'overdue_days', 'renewal_count', 'member_total_loans', 'member_overdue_count', 'member_total_fines']:
                if col in df.columns:
                    df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
            
            if 'overdue_days' in df.columns:
                df['days_overdue'] = df['overdue_days']
            else:
                df['days_overdue'] = 0
            
            if 'member_overdue_count' in df.columns:
                df['has_previous_overdue'] = (df['member_overdue_count'] > 0).astype(int)
            else:
                df['has_previous_overdue'] = 0
            
            df_train = df[df['fine_amount'] > 0].copy()
            if len(df_train) < 3:
                logger.warning(f"Only {len(df_train)} loans with fines")
                return False
            
            features = [f for f in self.feature_names if f in df_train.columns]
            X = df_train[features].fillna(0)
            y = df_train['fine_amount']
            
            self.model = LinearRegression()
            self.model.fit(X, y)
            
            with open(self.MODEL_PATH, 'wb') as f:
                pickle.dump(self.model, f)
            
            logger.info(f"Fine model trained on {len(df_train)} fined loans")
            return True
            
        except Exception as e:
            logger.error(f"Fine training failed: {e}")
            return False
    
    def predict(self, loan_id: Optional[str] = None, days_overdue: int = 0) -> List[Dict[str, Any]]:
        try:
            client = get_client()
            df = self._load_fine_data(client)
            if len(df) == 0:
                # If no fined loans, estimate based on days_overdue input
                if days_overdue > 0:
                    return [{
                        'loan_id': loan_id or 'N/A',
                        'estimated_fine': round(days_overdue * 0.5, 2),
                        'fine_rate_per_day': 0.5,
                        'days_overdue': days_overdue,
                        'note': 'heuristic_fallback_no_data'
                    }]
                return []
            
            for col in ['fine_amount', 'overdue_days', 'renewal_count', 'member_total_loans', 'member_overdue_count', 'member_total_fines']:
                if col in df.columns:
                    df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0)
            
            if 'overdue_days' in df.columns:
                df['days_overdue'] = df['overdue_days']
            else:
                df['days_overdue'] = 0
            
            if 'member_overdue_count' in df.columns:
                df['has_previous_overdue'] = (df['member_overdue_count'] > 0).astype(int)
            else:
                df['has_previous_overdue'] = 0
            
            # Train if needed
            if self.model is None:
                success = self.train(client)
                if not success:
                    return self._heuristic_predict(df, loan_id, days_overdue)
            
            features = [f for f in self.feature_names if f in df.columns]
            X = df[features].fillna(0)
            predictions = self.model.predict(X)
            df['expected_fine'] = np.clip(predictions, 0, None)
            
            if loan_id:
                df = df[df['loan_id'] == loan_id]
            
            results = []
            for _, row in df.iterrows():
                results.append({
                    'loan_id': str(row['loan_id']),
                    'user_id': str(row['user_id']),
                    'member_name': str(row.get('name', 'Unknown')),
                    'book_title': str(row.get('title', 'Unknown'))[:40],
                    'actual_fine': round(float(row.get('fine_amount', 0)), 2),
                    'expected_fine': round(float(row['expected_fine']), 2),
                    'days_overdue': int(row.get('days_overdue', 0)),
                    'member_overdue_count': int(row.get('member_overdue_count', 0))
                })
            
            return sorted(results, key=lambda x: x['expected_fine'], reverse=True)
            
        except Exception as e:
            logger.error(f"Fine prediction failed: {e}")
            return []
    
    def _heuristic_predict(self, df: pd.DataFrame, loan_id: Optional[str], days_overdue: int) -> List[Dict[str, Any]]:
        results = []
        for _, row in df.iterrows():
            days = int(row.get('days_overdue', days_overdue))
            fine = days * 0.5
            results.append({
                'loan_id': str(row.get('loan_id', loan_id or 'N/A')),
                'user_id': str(row.get('user_id', '')),
                'member_name': str(row.get('name', 'Unknown')),
                'book_title': str(row.get('title', 'Unknown'))[:40],
                'actual_fine': round(float(row.get('fine_amount', 0)), 2),
                'expected_fine': round(fine, 2),
                'days_overdue': days,
                'member_overdue_count': int(row.get('member_overdue_count', 0)),
                'note': 'heuristic_fallback'
            })
        return sorted(results, key=lambda x: x['expected_fine'], reverse=True)


# Singleton instances
_churn_predictor: Optional[ChurnPredictor] = None
_overdue_predictor: Optional[OverduePredictor] = None
_fine_estimator: Optional[FineEstimator] = None

def get_churn_predictor() -> ChurnPredictor:
    global _churn_predictor
    if _churn_predictor is None:
        _churn_predictor = ChurnPredictor()
    return _churn_predictor

def get_overdue_predictor() -> OverduePredictor:
    global _overdue_predictor
    if _overdue_predictor is None:
        _overdue_predictor = OverduePredictor()
    return _overdue_predictor

def get_fine_estimator() -> FineEstimator:
    global _fine_estimator
    if _fine_estimator is None:
        _fine_estimator = FineEstimator()
    return _fine_estimator
