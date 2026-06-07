"""
Standalone script to populate dim_date table in ClickHouse.
Run this after schema creation to fill the date dimension.
"""

import datetime as dt
import clickhouse_connect

def populate_dim_date(host='localhost', port=8123, database='library_dw'):
    client = clickhouse_connect.get_client(host=host, port=port, database=database)
    
    existing = client.query("SELECT count() FROM dim_date").result_rows[0][0]
    if existing > 0:
        print(f"dim_date already populated with {existing} rows, skipping")
        return existing

    day_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    month_names = ["January", "February", "March", "April", "May", "June",
                  "July", "August", "September", "October", "November", "December"]
    seasons = ["Winter", "Winter", "Spring", "Spring", "Spring", "Summer",
              "Summer", "Summer", "Autumn", "Autumn", "Autumn", "Winter"]

    rows = []
    start_date = dt.date(2020, 1, 1)
    end_date = dt.date(2030, 12, 31)
    current = start_date
    delta = dt.timedelta(days=1)

    while current <= end_date:
        date_key = int(current.strftime("%Y%m%d"))
        day_of_week = current.weekday()
        day_name = day_names[day_of_week]
        is_weekend = 1 if day_of_week >= 5 else 0
        is_holiday = 0
        md = (current.month, current.day)
        if md in [(1, 1), (7, 4), (12, 25), (12, 31)]:
            is_holiday = 1
        fiscal_q = ((current.month - 1) // 3) + 1

        rows.append({
            "date_key": date_key,
            "full_date": current,
            "day_of_week": day_of_week + 1,
            "day_name": day_name,
            "day_of_month": current.day,
            "day_of_year": current.timetuple().tm_yday,
            "week_of_year": current.isocalendar()[1],
            "month_number": current.month,
            "month_name": month_names[current.month - 1],
            "quarter": ((current.month - 1) // 3) + 1,
            "year": current.year,
            "fiscal_quarter": fiscal_q,
            "is_weekend": is_weekend,
            "is_holiday": is_holiday,
            "season": seasons[current.month - 1],
        })
        current += delta

    import pandas as pd
    df = pd.DataFrame(rows)
    client.insert_df("dim_date", df)
    print(f"dim_date: Inserted {len(rows)} rows (2020-01-01 to 2030-12-31)")
    return len(rows)

if __name__ == "__main__":
    populate_dim_date()
