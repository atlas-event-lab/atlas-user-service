-- user_profiles: the business profile Atlas owns, linked to the Keycloak identity.
-- keycloak_user_id (= JWT sub) is UNIQUE: it guarantees a single profile per identity
-- and makes the idempotent bootstrap safe under concurrency (DB-006, feature
-- Idempotency & Concurrency). PK column is `id` (DB-006).
CREATE TABLE user_profiles (
    id                UUID         NOT NULL,
    keycloak_user_id  VARCHAR(255) NOT NULL,
    email             VARCHAR(320) NOT NULL,
    first_name        VARCHAR(100),
    last_name         VARCHAR(100),
    display_name      VARCHAR(100),
    phone_number      VARCHAR(20),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_keycloak_user_id UNIQUE (keycloak_user_id),
    CONSTRAINT chk_user_profiles_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);
