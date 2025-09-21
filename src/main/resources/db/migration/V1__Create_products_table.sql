-- Create products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    shopify_product_id BIGINT UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    handle VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    product_type VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index on shopify_product_id for faster lookups
CREATE INDEX idx_products_shopify_id ON products(shopify_product_id);

-- Create index on title for search functionality
CREATE INDEX idx_products_title ON products(title);
