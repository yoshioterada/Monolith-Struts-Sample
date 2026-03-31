-- Add created_at column to security_logs table
ALTER TABLE security_logs ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
