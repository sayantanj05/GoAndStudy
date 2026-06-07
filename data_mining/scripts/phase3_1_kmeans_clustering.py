"""
Phase 3.1: K-Means Member Segmentation
Clusters members by behavior: total_loans, overdue_rate, fines, searches, ai_calls, age
"""

import os
import clickhouse_connect
import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase3_1"
os.makedirs(REPORTS_DIR, exist_ok=True)

def get_ch_client():
    return clickhouse_connect.get_client(
        host='localhost', port=8123,
        username='default', password='',
        database='library_dw'
    )

def run_query(query: str):
    ch = get_ch_client()
    try:
        result = ch.query(query)
        return result.result_rows, result.column_names
    finally:
        ch.close()

def main():
    print("=" * 60)
    print("PHASE 3.1: K-MEANS MEMBER SEGMENTATION")
    print("=" * 60)
    
    # Fetch member analytics
    print("\nFetching member analytics...")
    rows, cols = run_query("""
        SELECT 
            a.user_id,
            d.name,
            a.total_loans,
            a.total_overdue,
            a.overdue_rate,
            a.total_fines_incurred,
            a.total_fines_paid,
            a.total_searches,
            a.total_ai_calls,
            a.ai_conversion_rate,
            a.age
        FROM fact_member_analytics a
        JOIN dim_user d ON a.user_id = d.user_id
        WHERE a.total_loans > 0
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    print(f"Loaded {len(df)} members with loan activity")
    
    # Feature selection
    features = ['total_loans', 'total_overdue', 'overdue_rate', 
                'total_fines_incurred', 'total_searches', 'total_ai_calls', 'age']
    
    # Handle missing values
    for f in features:
        df[f] = pd.to_numeric(df[f], errors='coerce').fillna(0)
    
    X = df[features].values
    
    # Scale features
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # Determine optimal k using elbow method (try 2-5)
    print("\nTesting cluster counts...")
    inertias = []
    for k in range(2, 6):
        km = KMeans(n_clusters=k, random_state=42, n_init=10)
        km.fit(X_scaled)
        inertias.append(km.inertia_)
        print(f"  k={k}: inertia={km.inertia_:.2f}")
    
    # Use k=3 (good balance for small dataset)
    optimal_k = 3
    print(f"\nSelected k={optimal_k}")
    
    kmeans = KMeans(n_clusters=optimal_k, random_state=42, n_init=10)
    df['cluster'] = kmeans.fit_predict(X_scaled)
    
    # PCA for 2D visualization
    pca = PCA(n_components=2)
    X_pca = pca.fit_transform(X_scaled)
    df['pca_x'] = X_pca[:, 0]
    df['pca_y'] = X_pca[:, 1]
    
    # Cluster profiling
    print("\nCluster Profiles:")
    print("-" * 60)
    cluster_names = {}
    for c in range(optimal_k):
        cluster_df = df[df['cluster'] == c]
        avg_loans = cluster_df['total_loans'].mean()
        avg_overdue = cluster_df['overdue_rate'].mean()
        avg_fines = cluster_df['total_fines_incurred'].mean()
        avg_searches = cluster_df['total_searches'].mean()
        avg_age = cluster_df['age'].mean()
        count = len(cluster_df)
        
        # Name cluster based on characteristics
        if avg_loans > 20:
            name = "Power Readers"
        elif avg_loans > 5:
            name = "Regular Borrowers"
        else:
            name = "Casual / New"
        
        cluster_names[c] = name
        print(f"\n  Cluster {c}: {name} (n={count})")
        print(f"    Avg loans: {avg_loans:.1f}")
        print(f"    Avg overdue rate: {avg_overdue:.2%}")
        print(f"    Avg fines: {avg_fines:.2f}")
        print(f"    Avg searches: {avg_searches:.1f}")
        print(f"    Avg age: {avg_age:.1f}")
    
    df['cluster_name'] = df['cluster'].map(cluster_names)
    
    # Save report
    report_path = os.path.join(REPORTS_DIR, "member_clusters.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# K-Means Member Segmentation\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Method:** K-Means clustering (k={optimal_k}) on standardized features\n\n")
        f.write(f"**Features:** {', '.join(features)}\n\n")
        f.write(f"**Members analyzed:** {len(df)}\n\n")
        
        f.write("## Cluster Profiles\n\n")
        for c in range(optimal_k):
            cluster_df = df[df['cluster'] == c]
            f.write(f"### Cluster {c}: {cluster_names[c]} (n={len(cluster_df)})\n\n")
            f.write(f"| Metric | Average |\n")
            f.write(f"|---|---|\n")
            f.write(f"| Total Loans | {cluster_df['total_loans'].mean():.1f} |\n")
            f.write(f"| Overdue Rate | {cluster_df['overdue_rate'].mean():.2%} |\n")
            f.write(f"| Fines Incurred | {cluster_df['total_fines_incurred'].mean():.2f} |\n")
            f.write(f"| Searches | {cluster_df['total_searches'].mean():.1f} |\n")
            f.write(f"| AI Calls | {cluster_df['total_ai_calls'].mean():.1f} |\n")
            f.write(f"| Age | {cluster_df['age'].mean():.1f} |\n")
            f.write("\n")
            f.write("**Members:** " + ", ".join(cluster_df['name'].tolist()) + "\n\n")
        
        f.write("## PCA Visualization Coordinates\n\n")
        f.write("| Name | Cluster | PCA X | PCA Y |\n")
        f.write("|---|---|---|---|\n")
        for _, row in df.iterrows():
            f.write(f"| {row['name']} | {row['cluster_name']} | {row['pca_x']:.3f} | {row['pca_y']:.3f} |\n")
        f.write("\n")
        
        f.write("## Business Recommendations\n\n")
        f.write("- **Power Readers**: Offer premium membership, early access to new books\n")
        f.write("- **Regular Borrowers**: Send personalized recommendations, loyalty rewards\n")
        f.write("- **Casual / New**: Onboarding campaigns, popular book highlights\n")
        f.write("\n")
    
    # Save CSV
    csv_path = os.path.join(REPORTS_DIR, "member_clusters.csv")
    df[['user_id', 'name', 'cluster', 'cluster_name'] + features + ['pca_x', 'pca_y']].to_csv(csv_path, index=False)
    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 3.1 complete!")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
