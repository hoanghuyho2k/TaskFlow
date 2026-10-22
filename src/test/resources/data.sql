INSERT INTO users (id, email, display_name, password_hash, created_at)
VALUES (1, 'test-owner@example.invalid', 'Test Owner', '{bcrypt}$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuu', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('users', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM users), 1), true);
