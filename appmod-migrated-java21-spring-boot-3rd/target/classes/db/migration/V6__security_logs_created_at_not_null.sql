-- security_logs の created_at カラムに NOT NULL 制約を追加
ALTER TABLE security_logs ALTER COLUMN created_at SET NOT NULL;
