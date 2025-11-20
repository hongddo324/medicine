-- Add recipient_user_id column to activity table
-- This migration is needed because the column was added to the model but not to the database

-- Add the recipient_user_id column (nullable first to allow existing data)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS recipient_user_id BIGINT;

-- Update existing records to set recipient_user_id same as user_id (self-notification for old data)
UPDATE activity SET recipient_user_id = user_id WHERE recipient_user_id IS NULL;

-- Now make it non-nullable and add foreign key constraint
ALTER TABLE activity ALTER COLUMN recipient_user_id SET NOT NULL;

-- Add foreign key constraint
ALTER TABLE activity ADD CONSTRAINT fk_activity_recipient
    FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_recipient_user_id ON activity(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_recipient_created ON activity(recipient_user_id, created_at);

-- Verify the changes
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'activity' AND column_name = 'recipient_user_id';
