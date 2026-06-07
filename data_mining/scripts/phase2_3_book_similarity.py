"""
Phase 2.3: Content-Based Book Similarity
Computes cosine similarity between book embeddings for recommendations.
"""

import os
import clickhouse_connect
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from datetime import datetime

REPORTS_DIR = "data_mining/reports/phase2_3"
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

def get_book_embeddings_manual():
    """Fetch all embeddings and average in Python."""
    rows, cols = run_query("""
        SELECT book_id, title, embedding 
        FROM fact_book_embedding e
        JOIN dim_book b ON e.book_id = b.book_id
        ORDER BY book_id
    """)
    
    from collections import defaultdict
    book_embs = defaultdict(list)
    book_titles = {}
    
    for row in rows:
        book_id, title, embedding = row
        emb = np.array(embedding, dtype=np.float64)
        book_embs[book_id].append(emb)
        book_titles[book_id] = title
    
    # Average embeddings per book
    books = {}
    for book_id, embs in book_embs.items():
        avg_emb = np.mean(embs, axis=0)
        books[book_id] = {
            'title': book_titles[book_id],
            'embedding': avg_emb
        }
    
    # Find global max length and pad all embeddings
    max_len = max(len(books[bid]['embedding']) for bid in books)
    for bid in books:
        emb = books[bid]['embedding']
        if len(emb) < max_len:
            books[bid]['embedding'] = np.pad(emb, (0, max_len - len(emb)), mode='constant')
    
    return books

def compute_similarity_matrix(books):
    """Compute pairwise cosine similarity."""
    book_ids = list(books.keys())
    titles = [books[bid]['title'] for bid in book_ids]
    embeddings = np.array([books[bid]['embedding'] for bid in book_ids])
    
    sim_matrix = cosine_similarity(embeddings)
    return book_ids, titles, sim_matrix

def get_top_similar(sim_matrix, book_ids, titles, top_n=3):
    """Get top-N similar books for each book."""
    results = []
    for i, (bid, title) in enumerate(zip(book_ids, titles)):
        # Get similarities for this book (exclude self)
        sims = [(j, sim_matrix[i][j]) for j in range(len(book_ids)) if i != j]
        sims.sort(key=lambda x: x[1], reverse=True)
        
        top = []
        for j, sim in sims[:top_n]:
            top.append({
                'similar_book': titles[j],
                'similarity': round(float(sim), 3)
            })
        
        results.append({
            'book': title,
            'similar_books': top
        })
    
    return results

def save_similarity_report(results, book_ids, titles, sim_matrix):
    filepath = os.path.join(REPORTS_DIR, "book_similarity.md")
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write("# Content-Based Book Similarity\n\n")
        f.write(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("**Method:** Cosine similarity on averaged book embeddings\n\n")
        f.write("**Note:** With a small catalog (24 books), these similarities reflect content proximity and can guide 'Related Books' recommendations.\n\n")
        
        f.write("## Top Similar Book Pairs\n\n")
        f.write("| Book | Similar Book | Similarity Score |\n")
        f.write("|---|---|---|\n")
        for r in results:
            for s in r['similar_books']:
                f.write(f"| {r['book']} | {s['similar_book']} | {s['similarity']} |\n")
        f.write("\n")
        
        f.write("## Interpretation\n\n")
        f.write("- **Similarity > 0.9**: Very closely related content (same topic/author)\n")
        f.write("- **Similarity 0.7-0.9**: Related content (same genre/subject)\n")
        f.write("- **Similarity < 0.7**: Distinct content\n")
        f.write("\n")
    
    # Save CSV matrix
    csv_path = os.path.join(REPORTS_DIR, "similarity_matrix.csv")
    with open(csv_path, 'w', encoding='utf-8') as f:
        f.write("book," + ",".join(titles) + "\n")
        for i, title in enumerate(titles):
            f.write(title + "," + ",".join(f"{sim_matrix[i][j]:.3f}" for j in range(len(titles))) + "\n")
    
    print(f"✅ Saved: {filepath}")
    print(f"✅ Saved: {csv_path}")
    return filepath

def main():
    print("=" * 60)
    print("PHASE 2.3: BOOK SIMILARITY (EMBEDDINGS)")
    print("=" * 60)
    
    print("\nFetching book embeddings...")
    books = get_book_embeddings_manual()
    print(f"Found {len(books)} books with embeddings")
    
    print("Computing cosine similarity matrix...")
    book_ids, titles, sim_matrix = compute_similarity_matrix(books)
    
    print("Finding top similar books...")
    results = get_top_similar(sim_matrix, book_ids, titles, top_n=3)
    
    save_similarity_report(results, book_ids, titles, sim_matrix)
    
    # Print top findings
    print(f"\n{'=' * 60}")
    print("TOP SIMILAR BOOK PAIRS:")
    print(f"{'=' * 60}")
    for r in results[:5]:
        print(f"\n📚 {r['book']}")
        for s in r['similar_books']:
            print(f"   → {s['similar_book']} (sim={s['similarity']})")
    
    print(f"\n{'=' * 60}")
    print(f"✅ Phase 2.3 complete! Reports saved to: {REPORTS_DIR}")
    print(f"{'=' * 60}")

if __name__ == "__main__":
    main()
