-- Fix Array Types in ClickHouse
-- Convert author_ids and category_ids from String to Array(String)

-- Step 1: Add new columns with Array(String) type
ALTER TABLE library_dw.dim_book ADD COLUMN author_ids_new Array(String);
ALTER TABLE library_dw.dim_book ADD COLUMN category_ids_new Array(String);

-- Step 2: Parse string arrays to actual arrays
-- This removes the brackets and quotes, then splits by comma
ALTER TABLE library_dw.dim_book 
UPDATE author_ids_new = arrayMap(x -> trim(x), splitByString(',', replaceAll(author_ids, '[\\[\\]\\"\\']', '')))
WHERE length(author_ids) > 2;

ALTER TABLE library_dw.dim_book 
UPDATE category_ids_new = arrayMap(x -> trim(x), splitByString(',', replaceAll(category_ids, '[\\[\\]\\"\\']', '')))
WHERE length(category_ids) > 2;

-- Step 3: Handle empty arrays
ALTER TABLE library_dw.dim_book 
UPDATE author_ids_new = [] WHERE length(author_ids) <= 2;

ALTER TABLE library_dw.dim_book 
UPDATE category_ids_new = [] WHERE length(category_ids) <= 2;

-- Step 4: Drop old columns and rename new ones
ALTER TABLE library_dw.dim_book DROP COLUMN author_ids;
ALTER TABLE library_dw.dim_book DROP COLUMN category_ids;

ALTER TABLE library_dw.dim_book RENAME COLUMN author_ids_new TO author_ids;
ALTER TABLE library_dw.dim_book RENAME COLUMN category_ids_new TO category_ids;

-- Verification query
SELECT 
    isbn,
    title,
    author_ids,
    category_ids,
    toTypeName(author_ids) as author_ids_type,
    toTypeName(category_ids) as category_ids_type
FROM library_dw.dim_book 
LIMIT 5;
