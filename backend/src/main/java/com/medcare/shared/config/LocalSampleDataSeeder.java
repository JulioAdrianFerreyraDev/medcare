package com.medcare.shared.config;

import com.medcare.branch.ClinicBranchEntity;
import com.medcare.branch.ClinicBranchRepository;
import com.medcare.branch.PhysicalRoomEntity;
import com.medcare.branch.PhysicalRoomRepository;
import com.medcare.branch.StaffBranchAssignmentEntity;
import com.medcare.branch.StaffBranchAssignmentRepository;
import com.medcare.catalog.ClinicalServiceEntity;
import com.medcare.catalog.ClinicalServiceRepository;
import com.medcare.catalog.ServiceAppointmentEntity;
import com.medcare.catalog.ServiceAppointmentRepository;
import com.medcare.tenancy.BrandTenantEntity;
import com.medcare.tenancy.BrandTenantRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent local sample data seeder for Epic 1 UI & API integration testing.
 * Ensures that even if running without Flyway or on a fresh database,
 * 'Clínica Irma', its branches ('Sede Centro', 'Sede Norte'), rooms, staff assignments,
 * and Dr. Alejandro Vega's cardiology service catalog are ready for local testing.
 */
@Component
@Profile("!test")
public class LocalSampleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSampleDataSeeder.class);

    private static final String TENANT_SLUG = "clinica-irma";
    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SEDE_CENTRO_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SEDE_NORTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final String DR_VEGA_ID = "dr-alejandro-vega";

    private final BrandTenantRepository tenantRepository;
    private final ClinicBranchRepository branchRepository;
    private final PhysicalRoomRepository roomRepository;
    private final StaffBranchAssignmentRepository staffAssignmentRepository;
    private final ClinicalServiceRepository serviceRepository;
    private final ServiceAppointmentRepository appointmentRepository;

    public LocalSampleDataSeeder(
            BrandTenantRepository tenantRepository,
            ClinicBranchRepository branchRepository,
            PhysicalRoomRepository roomRepository,
            StaffBranchAssignmentRepository staffAssignmentRepository,
            ClinicalServiceRepository serviceRepository,
            ServiceAppointmentRepository appointmentRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.roomRepository = roomRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!tenantRepository.existsBySlug(TENANT_SLUG)) {
            tenantRepository.save(new BrandTenantEntity(
                    TENANT_ID,
                    "Clínica Irma",
                    "direccion@clinica-irma.com",
                    "+52 55 1234 5678",
                    TENANT_SLUG,
                    "ACTIVE",
                    true
            ));
            log.info("Seeded sample brand tenant: {} ({}.medika.com)", "Clínica Irma", TENANT_SLUG);
        }

        if (branchRepository.findAllByTenantSlug(TENANT_SLUG).isEmpty()) {
            branchRepository.saveAll(List.of(
                    new ClinicBranchEntity(
                            SEDE_CENTRO_ID,
                            TENANT_SLUG,
                            "Sede Centro",
                            "Av. Paseo de la Reforma 222, Juárez, Cuauhtémoc, 06600 CDMX",
                            "America/Mexico_City",
                            "+52 55 1122 3344",
                            true
                    ),
                    new ClinicBranchEntity(
                            SEDE_NORTE_ID,
                            TENANT_SLUG,
                            "Sede Norte",
                            "Av. Universidad 402, Nivel PB, Zona Norte, CDMX",
                            "America/Mexico_City",
                            "+52 55 8899 7766",
                            true
                    )
            ));

            roomRepository.saveAll(List.of(
                    new PhysicalRoomEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000201"),
                            TENANT_SLUG,
                            SEDE_CENTRO_ID,
                            "Consultorio Dental 1",
                            "DENTAL",
                            1
                    ),
                    new PhysicalRoomEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000202"),
                            TENANT_SLUG,
                            SEDE_CENTRO_ID,
                            "Cabina Podológica A",
                            "PODIATRY",
                            1
                    ),
                    new PhysicalRoomEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000203"),
                            TENANT_SLUG,
                            SEDE_CENTRO_ID,
                            "Consultorio General 2",
                            "GENERAL",
                            1
                    ),
                    new PhysicalRoomEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000204"),
                            TENANT_SLUG,
                            SEDE_NORTE_ID,
                            "Consultorio Dental 1 (Norte)",
                            "DENTAL",
                            1
                    ),
                    new PhysicalRoomEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000205"),
                            TENANT_SLUG,
                            SEDE_NORTE_ID,
                            "Consultorio Cardiología N2",
                            "CARDIOLOGY",
                            1
                    )
            ));
            log.info("Seeded sample branches (Sede Centro, Sede Norte) and 5 physical consultation rooms.");
        }

        if (staffAssignmentRepository.findAllByTenantSlug(TENANT_SLUG).isEmpty()) {
            staffAssignmentRepository.saveAll(List.of(
                    new StaffBranchAssignmentEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000401"),
                            TENANT_SLUG,
                            DR_VEGA_ID,
                            "Dr. Alejandro Vega",
                            "DOCTOR",
                            SEDE_CENTRO_ID.toString()
                    ),
                    new StaffBranchAssignmentEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000402"),
                            TENANT_SLUG,
                            DR_VEGA_ID,
                            "Dr. Alejandro Vega",
                            "DOCTOR",
                            SEDE_NORTE_ID.toString()
                    ),
                    new StaffBranchAssignmentEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000403"),
                            TENANT_SLUG,
                            "rec-laura-mendez",
                            "Laura Méndez",
                            "RECEPTIONIST",
                            SEDE_CENTRO_ID.toString()
                    ),
                    new StaffBranchAssignmentEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000404"),
                            TENANT_SLUG,
                            "dra-sofia-rincon",
                            "Dra. Sofía Rincón",
                            "DOCTOR",
                            SEDE_CENTRO_ID.toString()
                    )
            ));
            log.info("Seeded sample staff branch assignments for Dr. Alejandro Vega, Laura Méndez, and Dra. Sofía Rincón.");
        }

        if (serviceRepository.findAllByProviderId(DR_VEGA_ID).isEmpty()) {
            UUID ecoServiceId = UUID.fromString("00000000-0000-0000-0000-000000000301");
            serviceRepository.saveAll(List.of(
                    new ClinicalServiceEntity(
                            ecoServiceId,
                            TENANT_SLUG,
                            DR_VEGA_ID,
                            "Cardiología",
                            "Ecocardiograma Transtorácico",
                            60,
                            new BigDecimal("1500.00"),
                            "MXN",
                            "ACTIVE"
                    ),
                    new ClinicalServiceEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000302"),
                            TENANT_SLUG,
                            DR_VEGA_ID,
                            "Cardiología",
                            "Consulta de Primera Vez - Cardiología",
                            45,
                            new BigDecimal("800.00"),
                            "MXN",
                            "ACTIVE"
                    ),
                    new ClinicalServiceEntity(
                            UUID.fromString("00000000-0000-0000-0000-000000000303"),
                            TENANT_SLUG,
                            DR_VEGA_ID,
                            "Cardiología",
                            "Consulta de Seguimiento / Control",
                            20,
                            new BigDecimal("500.00"),
                            "MXN",
                            "INACTIVE"
                    )
            ));

            appointmentRepository.save(new ServiceAppointmentEntity(
                    UUID.fromString("00000000-0000-0000-0000-000000000501"),
                    TENANT_SLUG,
                    DR_VEGA_ID,
                    ecoServiceId,
                    "Ecocardiograma Transtorácico",
                    "Carlos Rivera",
                    Instant.parse("2026-11-16T10:00:00Z"),
                    Instant.parse("2026-11-16T11:00:00Z"),
                    60,
                    new BigDecimal("1500.00"),
                    "SCHEDULED"
            ));
            log.info("Seeded sample cardiology clinical services and 10:00-11:00 AM appointment for Dr. Alejandro Vega.");
        }
    }
}
