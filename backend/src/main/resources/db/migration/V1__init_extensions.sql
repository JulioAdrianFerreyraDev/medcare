-- V1__init_extensions.sql
-- Baseline migration enabling PostgreSQL extensions required by SRS v1.1.0
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "btree_gist";
