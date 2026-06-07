"""Feature engineering for recommendation ranking."""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import List


def compute_features(user_id: str, book_ids: List[str], feature_store) -> pd.DataFrame:
    """
    Compute ranker features for (user, book) pairs.
    Returns DataFrame with ~47 columns.
    """
    rows = []
    user_feats = feature_store.get_user_features(user_id)

    for book_id in book_ids:
        book_feats = feature_store.get_book_features(book_id)

        # --- User Features (20) ---
        u_total_loans = user_feats.get('total_loans', 0)
        u_active_loans = user_feats.get('active_loans', 0)
        u_days_since_last_loan = user_feats.get('days_since_last_loan', 999)
        u_avg_rating_given = user_feats.get('avg_rating_given', 0)
        u_engagement_score = user_feats.get('engagement_score', 0)
        u_genre_pref = user_feats.get('genre_preferences', {})

        # --- Book Features (16) ---
        b_total_ratings = book_feats.get('total_ratings', 0)
        b_avg_rating = book_feats.get('avg_rating', 0)
        b_trending_score = book_feats.get('trending_score', 0)
        b_recent_loans = book_feats.get('recent_loans', 0)
        b_wishlist_count = book_feats.get('wishlist_count', 0)
        b_freshness = book_feats.get('freshness_score', 1.0)
        b_genre = book_feats.get('genre', 'unknown')

        # --- Interaction Features (11) ---
        genre_match = u_genre_pref.get(b_genre, 0)
        two_tower_score = 0.0  # Phase 4 adds this
        time_match = 1.0  # Phase 5 adds session context

        rows.append({
            'user_id': user_id,
            'book_id': book_id,
            # User
            'u_total_loans': u_total_loans,
            'u_active_loans': u_active_loans,
            'u_days_since_last_loan': u_days_since_last_loan,
            'u_avg_rating_given': u_avg_rating_given,
            'u_engagement_score': u_engagement_score,
            # Book
            'b_total_ratings': b_total_ratings,
            'b_avg_rating': b_avg_rating,
            'b_trending_score': b_trending_score,
            'b_recent_loans': b_recent_loans,
            'b_wishlist_count': b_wishlist_count,
            'b_freshness': b_freshness,
            # Interaction
            'genre_match': genre_match,
            'two_tower_score': two_tower_score,
            'time_match': time_match,
            # Target (set by caller)
            'label': 0
        })

    return pd.DataFrame(rows)
