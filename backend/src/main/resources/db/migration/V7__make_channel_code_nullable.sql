-- V7: Make channel_code nullable in settlements
-- Derived from orders during matching; not always present in seed data
ALTER TABLE settlements ALTER COLUMN channel_code DROP NOT NULL;