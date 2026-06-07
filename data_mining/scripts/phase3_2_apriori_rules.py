"""
Phase 3.2: Apriori Association Rules
Discovers 'Users who borrowed X also borrowed Y' patterns.
"""

import os
import clickhouse_connect
import pandas as pd
import numpy as np
from mlxtend.frequent_patterns import apriori, association_rules
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase3_2"
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
    print("PHASE 3.2: APRIORI ASSOCIATION RULES")
    print("=" * 60)
    
    # Fetch loan transactions with book titles
    print("\nFetching loan transactions...")
    rows, cols = run_query("""
        SELECT 
            l.user_id,
            b.title
        FROM fact_loan l
        JOIN dim_book b ON l.book_id = b.book_id
        ORDER BY l.user_id
    """)
    
    df = pd.DataFrame(rows, columns=cols)
    print(f"Loaded {len(df)} loan transactions")
    
    if len(df) < 10:
        print("⚠️ Insufficient data for meaningful association rules")
        print("   Need at least 10+ transactions across multiple users")
        return
    
    # Create transaction matrix: one-hot encoded
    print("\nBuilding transaction matrix...")
    basket = df.groupby(['user_id', 'title']).size().unstack(fill_value=0)
    basket = (basket > 0).astype(bool)
    
    print(f"Matrix shape: {basket.shape} (users × books)")
    print(f"Users: {basket.shape[0]}, Unique books: {basket.shape[1]}")
    
    # Run Apriori with very low support for small dataset
    print("\nRunning Apriori algorithm...")
    min_support = max(0.01, 1 / len(basket))
    
    frequent_itemsets = apriori(basket, min_support=min_support, use_colnames=True, verbose=1)
    
    if len(frequent_itemsets) == 0:
        print("⚠️ No frequent itemsets found.")
        return
    
    print(f"Found {len(frequent_itemsets)} frequent itemsets")
    
    # Generate rules with very low threshold for small dataset
    print("\nGenerating association rules...")
    rules = association_rules(frequent_itemsets, metric="confidence", min_threshold=0.01)
    
    if len(rules) == 0:
        print("⚠️ No rules met confidence threshold. Reporting frequent itemsets only.")
        # Save frequent itemsets as fallback
        report_path = os.path.join(REPORTS_DIR, "association_rules.md")
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write("# Apriori Association Rules\n\n")
            f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            f.write("⚠️ **Note:** Only frequent itemsets found (no rules met confidence threshold)\n\n")
            f.write("This is typical for small datasets with concentrated borrowing patterns.\n\n")
            f.write("## Frequent Itemsets\n\n")
            f.write("| Itemset | Support |\n")
            f.write("|---|---|\n")
            for idx, row in frequent_itemsets.iterrows():
                items = ', '.join(list(row['itemsets']))
                f.write(f"| {items} | {row['support']:.3f} |\n")
        print(f"✅ Saved: {report_path}")
        return
    
    # Sort by lift
    rules = rules.sort_values('lift', ascending=False)
    print(f"Found {len(rules)} association rules")
    
    # Save report
    report_path = os.path.join(REPORTS_DIR, "association_rules.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Apriori Association Rules\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Method:** Apriori algorithm on loan transactions\n\n")
        f.write(f"**Transactions:** {len(df)} loans from {basket.shape[0]} users\n\n")
        f.write(f"**Min support:** {min_support:.3f}\n\n")
        f.write(f"**Min confidence:** 0.01 (lowered for small dataset)\n\n")
        
        f.write("## Top Association Rules\n\n")
        f.write("| If Borrowed (Antecedent) | Then Also Borrowed (Consequent) | Support | Confidence | Lift | Interpretation |\n")
        f.write("|---|---|---|---|---|---|\n")
        
        for idx, row in rules.head(20).iterrows():
            antecedent = ', '.join(list(row['antecedents']))
            consequent = ', '.join(list(row['consequents']))
            support = row['support']
            confidence = row['confidence']
            lift = row['lift']
            
            if lift > 2:
                interp = "Strong association"
            elif lift > 1.5:
                interp = "Moderate association"
            elif lift > 1:
                interp = "Weak association"
            else:
                interp = "No association"
            
            f.write(f"| {antecedent} | {consequent} | {support:.3f} | {confidence:.2%} | {lift:.2f} | {interp} |\n")
        
        f.write("\n")
        
        f.write("## Interpretation Guide\n\n")
        f.write("- **Support**: Fraction of all users who borrowed both books\n")
        f.write("- **Confidence**: Probability of borrowing consequent given antecedent\n")
        f.write("- **Lift > 1**: Books are more likely to be borrowed together than by chance\n")
        f.write("- **Lift > 2**: Strong recommendation candidate\n")
        f.write("\n")
        
        f.write("## Business Use Cases\n\n")
        f.write("1. **Bundle Recommendations**: Suggest consequent books on antecedent book pages\n")
        f.write("2. **Shelf Placement**: Place associated books near each other\n")
        f.write("3. **Email Campaigns**: 'Readers of X also enjoyed Y' personalized emails\n")
        f.write("4. **Inventory**: Ensure both books are well-stocked together\n")
        f.write("\n")
    
    # Save CSV
    csv_path = os.path.join(REPORTS_DIR, "association_rules.csv")
    rules_export = rules[['antecedents', 'consequents', 'support', 'confidence', 'lift']].copy()
    rules_export['antecedents'] = rules_export['antecedents'].apply(lambda x: '; '.join(list(x)))
    rules_export['consequents'] = rules_export['consequents'].apply(lambda x: '; '.join(list(x)))
    rules_export.to_csv(csv_path, index=False)
    
    print(f"\n✅ Saved: {report_path}")
    print(f"✅ Saved: {csv_path}")
    
    # Print top rules
    print(f"\n{'=' * 60}")
    print("TOP 5 ASSOCIATION RULES:")
    print(f"{'=' * 60}")
    for idx, row in rules.head(5).iterrows():
        ant = ', '.join(list(row['antecedents']))
        con = ', '.join(list(row['consequents']))
        print(f"\n📖 If '{ant}'")
        print(f"   → Then '{con}'")
        print(f"   Confidence: {row['confidence']:.1%}, Lift: {row['lift']:.2f}")
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 3.2 complete!")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
