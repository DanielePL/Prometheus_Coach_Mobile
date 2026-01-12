-- Run this first to see the actual column names:
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'coach_client_connections'
ORDER BY ordinal_position;
