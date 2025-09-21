-- Initialize PostgreSQL database for Greg Fullstack application
-- This script runs when the PostgreSQL container starts for the first time

-- Create the database if it doesn't exist (usually handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS greg_fullstack;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE greg_fullstack TO postgres;

-- Set timezone
SET timezone = 'UTC';
