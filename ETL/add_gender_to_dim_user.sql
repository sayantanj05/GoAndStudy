-- Add gender column to dim_user table
ALTER TABLE library_dw.dim_user 
ADD COLUMN gender Nullable(String) AFTER age;

-- Update existing records with gender from MongoDB (this will be done via Python script)
-- The Python script will handle the data migration
