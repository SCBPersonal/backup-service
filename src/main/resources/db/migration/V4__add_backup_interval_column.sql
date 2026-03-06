-- Migration: Add backup_interval column to store date ranges
-- Date: 2026-03-06
-- Description:
--   1. Adds backup_interval column to store date ranges (e.g., "2026-01-01 to 2026-01-31")
--   2. backup_period remains as simple identifier (e.g., "2026-03", "2026-W11") for matching base backups

-- Add backup_interval column to full_backup_tracker table to store date ranges
ALTER TABLE epricing.full_backup_tracker
ADD COLUMN backup_interval VARCHAR(100);

-- Add backup_interval column to incremental_backup_tracker table to store date ranges
ALTER TABLE epricing.incremental_backup_tracker
ADD COLUMN backup_interval VARCHAR(100);

-- Add comments to explain the columns
COMMENT ON COLUMN epricing.full_backup_tracker.backup_period IS 'Backup period identifier for matching (e.g., "2026-03" for MONTHLY, "2026-W11" for WEEKLY, "2026-01-01" for custom intervals)';
COMMENT ON COLUMN epricing.full_backup_tracker.backup_interval IS 'Backup date range covered (e.g., "2026-01-01 to 2026-01-31" for MONTHLY, "2026-01-06 to 2026-01-12" for WEEKLY)';

COMMENT ON COLUMN epricing.incremental_backup_tracker.backup_period IS 'Backup period identifier for matching (e.g., "2026-03" for MONTHLY, "2026-W11" for WEEKLY, "2026-01-01" for custom intervals)';
COMMENT ON COLUMN epricing.incremental_backup_tracker.backup_interval IS 'Backup date range covered (e.g., "2026-01-01 to 2026-01-31" for MONTHLY, "2026-01-06 to 2026-01-12" for WEEKLY)';

-- Create index on backup_interval for faster queries
CREATE INDEX idx_full_backup_interval ON epricing.full_backup_tracker(backup_interval);
CREATE INDEX idx_incremental_backup_interval ON epricing.incremental_backup_tracker(backup_interval);

