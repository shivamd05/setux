-- Seed Platform Admin
INSERT INTO management_users (
    organization_id,
    role_id,
    username,
    password_hash,
    enabled,
    created_at,
    updated_at
)
SELECT
    NULL,
    roles.id,
    'platform-admin',
    '$2y$10$E/Qaay/8upXkz0Qivswx.i9qy/DKCljpmBCfZZpLIDCo.SZnNyb0',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM roles
WHERE roles.name = 'PLATFORM_ADMIN';


-- Seed Gateway Service
INSERT INTO management_users (
    organization_id,
    role_id,
    username,
    password_hash,
    enabled,
    created_at,
    updated_at
)
SELECT
    NULL,
    roles.id,
    'gateway-service',
    '$2y$10$kgkpKYK3zLNzLV540bZbee68JvjRjy79J6kATSfD0grUXKDd6jx00',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM roles
WHERE roles.name = 'GATEWAY_SERVICE';