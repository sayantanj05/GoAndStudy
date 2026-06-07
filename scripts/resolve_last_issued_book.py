import os
import sys
import argparse
import datetime as dt

import requests


def norm_isbn(s: str) -> str:
    if s is None:
        return ""
    s = str(s).strip()
    return s.replace("-", "").replace(" ", "").upper()


def safe_get(dct, *keys, default=None):
    cur = dct
    for k in keys:
        if not isinstance(cur, dict) or k not in cur:
            return default
        cur = cur[k]
    return cur


def pick_latest_loan(loans):
    """
    loans: list of dicts potentially containing issuedAt/createdAt/returnedAt.
    """
    best = None
    best_ts = None

    for loan in loans or []:
        # prefer issuedAt, then returnedAt, then createdAt
        for k in ("issuedAt", "returnedAt", "createdAt", "updatedAt"):
            ts = loan.get(k)
            if ts:
                try:
                    # support ISO timestamps
                    if isinstance(ts, str):
                        t = dt.datetime.fromisoformat(ts.replace("Z", "+00:00"))
                    else:
                        t = dt.datetime.fromtimestamp(ts)
                    break
                except Exception:
                    t = None
            else:
                t = None

        if not t:
            continue

        if best_ts is None or t > best_ts:
            best_ts = t
            best = loan

    return best


def query_via_api(base_url, member_id, token, max_pages=3):
    """
    No guaranteed 'last issued' endpoint exists in the repo, so we use the member history endpoint
    (returns paginated RETURNED loans) and pick the latest timestamp we can see.
    If the member's latest loan is still active, history may not include it.
    """
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    latest = None

    all_loans = []
    for page in range(1, max_pages + 1):
        url = f"{base_url}/api/v1/member/history?page={page}"
        r = requests.get(url, headers=headers, timeout=30)
        r.raise_for_status()
        payload = r.json()

        # payload shape in this project: ApiResponse.success("...", <object>)
        # We'll robustly locate "History fetched" data.
        data = payload.get("data", None)
        if data is None:
            # sometimes they may nest
            data = payload.get("response", None) or payload

        loans = None
        if isinstance(data, dict):
            loans = data.get("loans") or data.get("items") or data.get("content") or data.get("data")
        if loans is None:
            # try if payload itself contains "loans"
            loans = payload.get("loans")

        if loans is None:
            loans = []

        all_loans.extend(loans)

        # stop early if we got fewer than a full page and this is the last page
        if isinstance(loans, list) and len(loans) == 0:
            break

    latest = pick_latest_loan(all_loans)
    return latest


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default=os.environ.get("BASE_URL", "").strip() or "http://localhost:8080", help="Backend base URL")
    ap.add_argument("--member-id", default=os.environ.get("MEMBER_ID", "MEM08052026001"))
    ap.add_argument("--email", default=os.environ.get("MEMBER_EMAIL", "sayan@gmail.com"))
    ap.add_argument("--token", default=os.environ.get("AUTH_TOKEN", ""), help="Bearer token to call protected endpoints (optional)")
    ap.add_argument("--max-pages", type=int, default=int(os.environ.get("MAX_PAGES", "3")), help="How many pages of history to fetch")
    args = ap.parse_args()

    try:
        latest = query_via_api(args.base_url.rstrip("/"), args.member_id, args.token, max_pages=args.max_pages)
    except requests.RequestException as e:
        print("ERROR calling backend API:", str(e))
        sys.exit(2)

    if not latest:
        print(f"No loan record found for memberId={args.member_id} using the available API history endpoint.")
        sys.exit(0)

    book_title = latest.get("bookTitle") or latest.get("title") or ""
    book_isbn = latest.get("bookIsbn") or latest.get("isbn") or ""
    loan_id = latest.get("id") or latest.get("loanId") or ""
    issued_at = latest.get("issuedAt") or latest.get("createdAt") or ""

    print("Last issued (best-effort from available history pages):")
    print(f"- memberId: {args.member_id}")
    print(f"- bookTitle: {book_title}")
    print(f"- bookIsbn: {book_isbn} (normalized: {norm_isbn(book_isbn)})")
    print(f"- loanId: {loan_id}")
    print(f"- issuedAt/createdAt: {issued_at}")


if __name__ == "__main__":
    main()
