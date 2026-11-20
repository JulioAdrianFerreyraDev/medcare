-- V5__clinic_flow_and_reception_calendar_schema.sql
-- Multi-Doctor Calendar Matrix (AHC-45)
-- Patient Check-In & Clinic Flow Tracking (AHC-46)
-- Patient Self-Service Booking Portal (AHC-47)

ALTER TABLE scheduled_appointments
    ADD COLUMN IF NOT EXISTS service_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS specialty VARCHAR(128),
    ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS consultation_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS consultation_completed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_scheduled_appointments_calendar
    ON scheduled_appointments(tenant_slug, branch_id, start_time);

CREATE INDEX IF NOT EXISTS idx_scheduled_appointments_status
    ON scheduled_appointments(tenant_slug, status);
