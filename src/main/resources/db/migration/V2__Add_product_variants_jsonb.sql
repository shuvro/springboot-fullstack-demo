ALTER TABLE products
    ADD COLUMN variants JSONB NOT NULL DEFAULT '[]'::jsonb;
