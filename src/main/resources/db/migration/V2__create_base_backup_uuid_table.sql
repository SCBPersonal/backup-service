-- Create table to store base backup UUIDs monthly
CREATE TABLE IF NOT EXISTS epricing.base_backup_uuid_tracker (
    id SERIAL PRIMARY KEY,
    category_code VARCHAR(255) NOT NULL,
    base_backup_uuid VARCHAR(255) NOT NULL,
    backup_month VARCHAR(7) NOT NULL,  -- Format: YYYY-MM
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(category_code, backup_month)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_base_backup_category_month 
ON epricing.base_backup_uuid_tracker(category_code, backup_month);

-- Add comment to table
COMMENT ON TABLE epricing.base_backup_uuid_tracker IS 'Stores base backup UUIDs for incremental backups, organized by month';
COMMENT ON COLUMN epricing.base_backup_uuid_tracker.category_code IS 'Backup category code (e.g., HWA_EPR_DB_BACKUP_FULL)';
COMMENT ON COLUMN epricing.base_backup_uuid_tracker.base_backup_uuid IS 'UUID of the base backup from YBA';
COMMENT ON COLUMN epricing.base_backup_uuid_tracker.backup_month IS 'Month of the backup in YYYY-MM format';

