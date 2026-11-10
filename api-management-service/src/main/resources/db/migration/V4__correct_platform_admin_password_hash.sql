-- Replace the malformed seed hash with the tutorial's BCrypt hash for platform123.
UPDATE management_users
SET password_hash = '$2y$10$E/Qaay/8upXkzOQivswrx.i9qy/DkCljpmBCfZZpLIDCo.SZnNybO'
WHERE username = 'platform-admin';
