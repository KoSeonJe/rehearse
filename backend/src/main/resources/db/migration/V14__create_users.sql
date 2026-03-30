CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    profile_image VARCHAR(500),
    provider    VARCHAR(20)  NOT NULL COMMENT 'GITHUB | GOOGLE',
    provider_id VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
