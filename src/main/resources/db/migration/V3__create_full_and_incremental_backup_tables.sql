-- Create table for full backup tracking
CREATE TABLE IF NOT EXISTS epricing.full_backup_tracker (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    category_code VARCHAR(255) NOT NULL,
    business_date DATE NOT NULL,
    backup_month VARCHAR(7) NOT NULL,  -- Format: YYYY-MM
    backup_status VARCHAR(50) NOT NULL,  -- IN_PROGRESS, SUCCESS, FAILED
    full_backup_response TEXT,  -- YBA API response JSON
    base_backup_uuid VARCHAR(255),  -- Populated after job completion
    task_uuid VARCHAR(255),  -- YBA task UUID for polling
    error_message TEXT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(category_code, backup_month)
);

-- Create table for incremental backup tracking
CREATE TABLE IF NOT EXISTS epricing.incremental_backup_tracker (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    category_code VARCHAR(255) NOT NULL,
    business_date DATE NOT NULL,
    backup_month VARCHAR(7) NOT NULL,  -- Format: YYYY-MM
    base_backup_uuid VARCHAR(255) NOT NULL,  -- Reference to full backup
    backup_status VARCHAR(50) NOT NULL,  -- IN_PROGRESS, SUCCESS, FAILED
    incremental_backup_response TEXT,  -- YBA API response JSON
    task_uuid VARCHAR(255),  -- YBA task UUID
    error_message TEXT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_full_backup_category_month 
ON epricing.full_backup_tracker(category_code, backup_month);

CREATE INDEX IF NOT EXISTS idx_full_backup_status 
ON epricing.full_backup_tracker(backup_status);

CREATE INDEX IF NOT EXISTS idx_full_backup_task_uuid 
ON epricing.full_backup_tracker(task_uuid);

CREATE INDEX IF NOT EXISTS idx_incremental_backup_category_date 
ON epricing.incremental_backup_tracker(category_code, business_date);

CREATE INDEX IF NOT EXISTS idx_incremental_backup_base_uuid 
ON epricing.incremental_backup_tracker(base_backup_uuid);

-- Add comments to tables
COMMENT ON TABLE epricing.full_backup_tracker IS 'Tracks full backup operations with base UUID populated after job completion';
COMMENT ON COLUMN epricing.full_backup_tracker.batch_id IS 'Batch execution ID';
COMMENT ON COLUMN epricing.full_backup_tracker.category_code IS 'Backup category code (e.g., HWA_EPR_DB_BACKUP_FULL)';
COMMENT ON COLUMN epricing.full_backup_tracker.backup_month IS 'Month of the backup in YYYY-MM format';
COMMENT ON COLUMN epricing.full_backup_tracker.base_backup_uuid IS 'UUID of the base backup, populated after job completion';
COMMENT ON COLUMN epricing.full_backup_tracker.task_uuid IS 'YBA task UUID for polling job status';

COMMENT ON TABLE epricing.incremental_backup_tracker IS 'Tracks incremental backup operations';
COMMENT ON COLUMN epricing.incremental_backup_tracker.base_backup_uuid IS 'Reference to the base backup UUID from full_backup_tracker';

