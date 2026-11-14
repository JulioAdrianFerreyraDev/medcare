-- V2__tenancy_branches_and_catalog_schema.sql
-- Multi-Tenancy (AHC-3), Multi-Branch & Physical Rooms (AHC-4), Provider Services Catalog (AHC-5)

-- 1. Brand Tenants (AHC-3)
CREATE TABLE IF NOT EXISTS brand_tenants (
    id UUID PRIMARY KEY,
    legal_business_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(64) NOT NULL,
    slug VARCHAR(63) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    activation_email_dispatched BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_brand_tenants_slug ON brand_tenants(slug);

-- 2. Clinic Branches / Sedes (AHC-4 AC-1)
CREATE TABLE IF NOT EXISTS clinic_branches (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clinic_branches_tenant ON clinic_branches(tenant_slug);

-- 3. Physical Consultation Rooms / Consultorios (AHC-4 AC-2, AHC-16)
CREATE TABLE IF NOT EXISTS physical_rooms (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    branch_id UUID NOT NULL REFERENCES clinic_branches(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    room_type VARCHAR(64) NOT NULL,
    concurrency_capacity INT NOT NULL DEFAULT 1 CHECK (concurrency_capacity = 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_physical_rooms_branch ON physical_rooms(tenant_slug, branch_id);

-- 4. Room Reservations with PostgreSQL btree_gist temporal exclusion (AHC-16)
CREATE TABLE IF NOT EXISTS room_reservations (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    branch_id UUID NOT NULL REFERENCES clinic_branches(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES physical_rooms(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT chk_room_reservation_time CHECK (end_time > start_time)
);

ALTER TABLE room_reservations ADD CONSTRAINT no_room_overlapping_slots
EXCLUDE USING gist (
    room_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status NOT IN ('CANCELLED'));

-- 5. Multi-Branch Staff Assignments (AHC-4 AC-3, AHC-17)
CREATE TABLE IF NOT EXISTS staff_branch_assignments (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    staff_id VARCHAR(128) NOT NULL,
    staff_name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    branch_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_staff_assignments_tenant_staff ON staff_branch_assignments(tenant_slug, staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_assignments_tenant_branch ON staff_branch_assignments(tenant_slug, branch_id);

-- 6. Provider Clinical Services & Pricing Catalog (AHC-5 AC-1, AC-3)
CREATE TABLE IF NOT EXISTS clinical_services (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    specialty VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes >= 5 AND duration_minutes <= 480),
    fee_amount NUMERIC(12, 2) NOT NULL CHECK (fee_amount > 0),
    currency VARCHAR(16) NOT NULL DEFAULT 'MXN',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clinical_services_provider ON clinical_services(tenant_slug, provider_id, status);

-- 7. Service Appointments / Continuous Time Block Allocations (AHC-5 AC-2, AC-3)
CREATE TABLE IF NOT EXISTS service_appointments (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(63) NOT NULL,
    provider_id VARCHAR(128) NOT NULL,
    service_id UUID NOT NULL REFERENCES clinical_services(id),
    service_name_snapshot VARCHAR(255) NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL,
    billed_fee NUMERIC(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_appointment_time CHECK (end_time > start_time)
);

ALTER TABLE service_appointments ADD CONSTRAINT no_doctor_overlapping_slots
EXCLUDE USING gist (
    provider_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status NOT IN ('CANCELLED'));
