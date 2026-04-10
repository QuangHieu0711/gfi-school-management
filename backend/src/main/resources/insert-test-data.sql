-- Insert test user
-- Password: admin123 (BCrypt hash)
INSERT INTO users (username, password, full_name, email, phone, role_id, unit_id, status, created_at, created_by)
VALUES ('admin', '$2a$10$3/bGaH/Y3vI9P51qWZwBCu5ZdCpH2RWzqrjUTxHv1jYcJP.aDvSly', 'Admin User', 'admin@example.com', '0123456789', 3, 1, 1, NOW(), 'system')
ON CONFLICT (username) DO NOTHING;
