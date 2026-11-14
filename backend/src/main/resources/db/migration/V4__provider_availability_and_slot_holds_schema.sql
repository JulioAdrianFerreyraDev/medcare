-- V4__provider_availability_and_slot_holds_schema.sql
-- Provider Working Hours & Shift Configuration (AHC-6),
-- Planned Time-Off & Emergency Provider Cancellations (AHC-7),
-- Optimistic Slot Hold & Authenticated Double-Booking Prevention (AHC-8)

-- 1. Provider Weekly Working Shifts per Branch (AHC-6 AC-1, AC-2)
CREATE TABLE IF NOT EXISTS provider_shifts (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    branch_id VARCHAR(128) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(16) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_interval_minutes INT NOT NULL DEFAULT 30 CHECK (slot_interval_minutes >= 5 AND slot_interval_minutes <= 240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_provider_shift_window CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_provider_shifts_lookup
    ON provider_shifts(tenant_slug, provider_id, day_of_week);

-- 2. Provider Planned Time-Off & Emergency Cancellations (AHC-7 AC-1, AC-2)
CREATE TABLE IF NOT EXISTS provider_time_off_blocks (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    branch_id VARCHAR(128) NOT NULL,
    block_type VARCHAR(32) NOT NULL, -- 'PLANNED_LEAVE' | 'EMERGENCY_CANCELLATION'
    start_date_time TIMESTAMPTZ NOT NULL,
    end_date_time TIMESTAMPTZ NOT NULL,
    reason VARCHAR(500) NOT NULL,
    affected_appointments_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_time_off_range CHECK (end_date_time > start_date_time)
);

CREATE INDEX IF NOT EXISTS idx_provider_time_off_lookup
    ON provider_time_off_blocks(tenant_slug, provider_id, start_date_time, end_date_time);

-- 3. Immutable Cancellation & Schedule Block Audit Trail (AHC-7 AC-3)
CREATE TABLE IF NOT EXISTS schedule_cancellation_audit_logs (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    block_id UUID NOT NULL REFERENCES provider_time_off_blocks(id),
    action_type VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    affected_appointments_count INT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cancellation_audit_provider
    ON schedule_cancellation_audit_logs(tenant_slug, provider_id, recorded_at DESC);

-- 4. Receptionist Dashboard Alerts for Emergency Rescheduling (AHC-7 AC-2)
CREATE TABLE IF NOT EXISTS receptionist_dashboard_alerts (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    branch_id VARCHAR(128) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'HIGH',
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    affected_appointments_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_receptionist_alerts_branch
    ON receptionist_dashboard_alerts(tenant_slug, branch_id, status);

-- 5. Confirmed Scheduled Appointments with Optimistic Hold Token & btree_gist Exclusion (AHC-8 AC-2, AC-4)
CREATE TABLE IF NOT EXISTS scheduled_appointments (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    branch_id VARCHAR(128) NOT NULL,
    room_id VARCHAR(128) NOT NULL,
    doctor_id VARCHAR(128) NOT NULL,
    patient_id VARCHAR(128) NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    patient_contact VARCHAR(255) NOT NULL,
    hold_token VARCHAR(128) NOT NULL UNIQUE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED' | 'REQUIRES_RESCHEDULING' | 'CANCELLED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_scheduled_appointment_time CHECK (end_time > start_time)
);

ALTER TABLE scheduled_appointments ADD CONSTRAINT no_scheduled_doctor_overlap
EXCLUDE USING gist (
    doctor_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING'));

ALTER TABLE scheduled_appointments ADD CONSTRAINT no_scheduled_room_overlap
EXCLUDE USING gist (
    room_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status NOT IN ('CANCELLED', 'REQUIRES_RESCHEDULING'));

-- 6. Seed Sample Weekly Working Shifts for Dr. Alejandro Vega at Sede Norte (AHC-6 AC-1)
INSERT INTO provider_shifts (
    id,
    tenant_slug,
    provider_id,
    branch_id,
    branch_name,
    day_of_week,
    start_time,
    end_time,
    slot_interval_minutes
) VALUES
    (
        '00000000-0000-0000-0000-000000000601',
        'clinica-irma',
        'dr-alejandro-vega',
        '00000000-0000-0000-0000-000000000102',
        'Sede Norte',
        'TUESDAY',
        '09:00:00',
        '13:00:00',
        60
    ),
    (
        '00000000-0000-0000-0000-000000000602',
        'clinica-irma',
        'dr-alejandro-vega',
        '00000000-0000-0000-0000-000000000102',
        'Sede Norte',
        'TUESDAY',
        '15:00:00',
        '19:00:00',
        60
    )
ON CONFLICT (id) DO NOTHING;
