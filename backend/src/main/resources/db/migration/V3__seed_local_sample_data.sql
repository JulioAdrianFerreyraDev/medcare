-- ============================================================================
-- Flyway Migration V3: Seed Local Sample Data for Epic 1 UI & API Testing
-- Tenant: Clínica Irma (clinica-irma.medika.com)
-- Branches: Sede Centro (Matriz), Sede Norte (Sucursal)
-- Consultation Rooms, Multi-Branch Staff Assignments, Cardiology Services & Appointments
-- ============================================================================

-- 1. Seed Brand Tenant (AHC-3)
INSERT INTO brand_tenants (
    id,
    legal_business_name,
    contact_email,
    contact_phone,
    slug,
    status,
    activation_email_dispatched
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Clínica Irma',
    'direccion@clinica-irma.com',
    '+52 55 1234 5678',
    'clinica-irma',
    'ACTIVE',
    TRUE
) ON CONFLICT (slug) DO NOTHING;

-- 2. Seed Clinic Branches (AHC-4)
INSERT INTO clinic_branches (
    id,
    tenant_slug,
    name,
    address,
    timezone,
    contact_phone,
    active
) VALUES
    (
        '00000000-0000-0000-0000-000000000101',
        'clinica-irma',
        'Sede Centro',
        'Av. Paseo de la Reforma 222, Juárez, Cuauhtémoc, 06600 CDMX',
        'America/Mexico_City',
        '+52 55 1122 3344',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000000102',
        'clinica-irma',
        'Sede Norte',
        'Av. Universidad 402, Nivel PB, Zona Norte, CDMX',
        'America/Mexico_City',
        '+52 55 8899 7766',
        TRUE
    )
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Physical Consultation Rooms (Capacity = 1) (AHC-4)
INSERT INTO physical_rooms (
    id,
    tenant_slug,
    branch_id,
    name,
    room_type,
    concurrency_capacity
) VALUES
    (
        '00000000-0000-0000-0000-000000000201',
        'clinica-irma',
        '00000000-0000-0000-0000-000000000101',
        'Consultorio Dental 1',
        'DENTAL',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000202',
        'clinica-irma',
        '00000000-0000-0000-0000-000000000101',
        'Cabina Podológica A',
        'PODIATRY',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000203',
        'clinica-irma',
        '00000000-0000-0000-0000-000000000101',
        'Consultorio General 2',
        'GENERAL',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000204',
        'clinica-irma',
        '00000000-0000-0000-0000-000000000102',
        'Consultorio Dental 1 (Norte)',
        'DENTAL',
        1
    ),
    (
        '00000000-0000-0000-0000-000000000205',
        'clinica-irma',
        '00000000-0000-0000-0000-000000000102',
        'Consultorio Cardiología N2',
        'CARDIOLOGY',
        1
    )
ON CONFLICT (id) DO NOTHING;

-- 4. Seed Multi-Branch Staff Assignments (AHC-4)
-- Doctors can operate across multiple branches; Receptionists are strictly bound to 1 branch.
INSERT INTO staff_branch_assignments (
    id,
    tenant_slug,
    staff_id,
    staff_name,
    role,
    branch_id
) VALUES
    (
        '00000000-0000-0000-0000-000000000401',
        'clinica-irma',
        'dr-alejandro-vega',
        'Dr. Alejandro Vega',
        'DOCTOR',
        '00000000-0000-0000-0000-000000000101'
    ),
    (
        '00000000-0000-0000-0000-000000000402',
        'clinica-irma',
        'dr-alejandro-vega',
        'Dr. Alejandro Vega',
        'DOCTOR',
        '00000000-0000-0000-0000-000000000102'
    ),
    (
        '00000000-0000-0000-0000-000000000403',
        'clinica-irma',
        'rec-laura-mendez',
        'Laura Méndez',
        'RECEPTIONIST',
        '00000000-0000-0000-0000-000000000101'
    ),
    (
        '00000000-0000-0000-0000-000000000404',
        'clinica-irma',
        'dra-sofia-rincon',
        'Dra. Sofía Rincón',
        'DOCTOR',
        '00000000-0000-0000-0000-000000000101'
    )
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Provider Clinical Services & Pricing Catalog (AHC-5)
INSERT INTO clinical_services (
    id,
    tenant_slug,
    provider_id,
    specialty,
    name,
    duration_minutes,
    fee_amount,
    currency,
    status
) VALUES
    (
        '00000000-0000-0000-0000-000000000301',
        'clinica-irma',
        'dr-alejandro-vega',
        'Cardiología',
        'Ecocardiograma Transtorácico',
        60,
        1500.00,
        'MXN',
        'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000302',
        'clinica-irma',
        'dr-alejandro-vega',
        'Cardiología',
        'Consulta de Primera Vez - Cardiología',
        45,
        800.00,
        'MXN',
        'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000303',
        'clinica-irma',
        'dr-alejandro-vega',
        'Cardiología',
        'Consulta de Seguimiento / Control',
        20,
        500.00,
        'MXN',
        'INACTIVE'
    )
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Sample Occupied Appointment Slot (10:00 AM - 11:00 AM) (AHC-5)
INSERT INTO service_appointments (
    id,
    tenant_slug,
    provider_id,
    service_id,
    service_name_snapshot,
    patient_name,
    start_time,
    end_time,
    duration_minutes,
    billed_fee,
    status
) VALUES (
    '00000000-0000-0000-0000-000000000501',
    'clinica-irma',
    'dr-alejandro-vega',
    '00000000-0000-0000-0000-000000000301',
    'Ecocardiograma Transtorácico',
    'Carlos Rivera',
    '2026-11-16T10:00:00Z',
    '2026-11-16T11:00:00Z',
    60,
    1500.00,
    'SCHEDULED'
) ON CONFLICT (id) DO NOTHING;
