INSERT INTO roles (id, name, description)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'USER', 'Default user role'),
    ('22222222-2222-2222-2222-222222222222', 'ADMIN', 'Administrative role')
ON CONFLICT (name) DO NOTHING;
