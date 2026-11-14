package com.medcare.branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medcare.branch.BranchManagementService.AssignStaffCommand;
import com.medcare.branch.BranchManagementService.BranchResponse;
import com.medcare.branch.BranchManagementService.CreateBranchCommand;
import com.medcare.branch.BranchManagementService.CreateRoomCommand;
import com.medcare.branch.BranchManagementService.PhysicalRoomResponse;
import com.medcare.branch.BranchManagementService.StaffAssignmentResponse;
import com.medcare.shared.exception.DomainException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * [AHC-18] JUnit 5 Unit Tests for Story AHC-4:
 * Multi-Branch & Physical Room Management.
 */
@ExtendWith(MockitoExtension.class)
class BranchAndRoomManagementUnitTest {

    @Mock
    private ClinicBranchRepository branchRepository;

    @Mock
    private PhysicalRoomRepository roomRepository;

    @Mock
    private RoomReservationRepository reservationRepository;

    @Mock
    private StaffBranchAssignmentRepository staffAssignmentRepository;

    private BranchManagementService branchManagementService;

    @BeforeEach
    void setUp() {
        branchManagementService = new BranchManagementService(
                branchRepository,
                roomRepository,
                reservationRepository,
                staffAssignmentRepository
        );
    }

    @Test
    @DisplayName("AC-1: Should register a new clinic branch under the active brand tenant with valid timezone")
    void shouldRegisterBranchUnderActiveTenantWithValidTimezone() {
        var command = new CreateBranchCommand(
                "clinica-irma",
                "Sede Centro",
                "Av. Reforma 222, CDMX",
                "America/Mexico_City",
                "+525511223344"
        );
        when(branchRepository.save(any(ClinicBranchEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BranchResponse savedBranch = branchManagementService.registerBranch(command);

        assertThat(savedBranch).isNotNull();
        assertThat(savedBranch.tenantSlug()).isEqualTo("clinica-irma");
        assertThat(savedBranch.name()).isEqualTo("Sede Centro");
        assertThat(savedBranch.timezone()).isEqualTo("America/Mexico_City");
        assertThat(savedBranch.active()).isTrue();
        verify(branchRepository).save(any(ClinicBranchEntity.class));
    }

    @Test
    @DisplayName("AC-1: Should reject branch registration when timezone identifier is invalid")
    void shouldRejectBranchRegistrationWithInvalidTimezone() {
        var command = new CreateBranchCommand(
                "clinica-irma",
                "Sede Sur",
                "Av. Insurgentes 100",
                "Invalid/Timezone_ID",
                "+525511223344"
        );

        assertThatThrownBy(() -> branchManagementService.registerBranch(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getMessage()).contains("Invalid IANA timezone");
                });

        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-2: Should register physical consultation rooms with strict concurrency capacity of 1")
    void shouldRegisterPhysicalRoomWithStrictConcurrencyCapacityOfOne() {
        UUID branchId = UUID.randomUUID();
        when(branchRepository.existsByIdAndTenantSlug(branchId, "clinica-irma")).thenReturn(true);
        when(roomRepository.save(any(PhysicalRoomEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateRoomCommand("clinica-irma", branchId, "Consultorio Dental 1", "DENTAL", 1);

        PhysicalRoomResponse room = branchManagementService.registerPhysicalRoom(command);

        assertThat(room.name()).isEqualTo("Consultorio Dental 1");
        assertThat(room.concurrencyCapacity()).isEqualTo(1);
        verify(roomRepository).save(any(PhysicalRoomEntity.class));
    }

    @Test
    @DisplayName("AC-2: Should reject physical room creation if requested concurrency capacity is not equal to 1")
    void shouldRejectPhysicalRoomWhenCapacityViolatesSingleConsultationInvariant() {
        UUID branchId = UUID.randomUUID();
        var invalidCommand = new CreateRoomCommand("clinica-irma", branchId, "Cabina Podológica A", "PODIATRY", 2);

        assertThatThrownBy(() -> branchManagementService.registerPhysicalRoom(invalidCommand))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getMessage()).contains("concurrency capacity of 1");
                });

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-3: Should allow assigning a DOCTOR across multiple branches ('Sede Centro' and 'Sede Norte')")
    void shouldAllowAssigningDoctorToMultipleBranches() {
        String doctorId = UUID.randomUUID().toString();
        String sedeCentroId = UUID.randomUUID().toString();
        String sedeNorteId = UUID.randomUUID().toString();
        when(staffAssignmentRepository.findAllByTenantSlugAndStaffId("clinica-irma", doctorId))
                .thenReturn(List.of(new StaffBranchAssignmentEntity(
                        UUID.randomUUID(), "clinica-irma", doctorId, "Dr. Vega", "DOCTOR", sedeCentroId
                )));

        var command = new AssignStaffCommand("clinica-irma", doctorId, "Dr. Vega", "DOCTOR", sedeNorteId);

        StaffAssignmentResponse assignment = branchManagementService.assignStaffToBranch(command);

        assertThat(assignment.staffId()).isEqualTo(doctorId);
        assertThat(assignment.role()).isEqualTo("DOCTOR");
        assertThat(assignment.assignedBranches()).containsExactlyInAnyOrder(sedeCentroId, sedeNorteId);
        verify(staffAssignmentRepository).save(any(StaffBranchAssignmentEntity.class));
    }

    @Test
    @DisplayName("AC-3: Should reject assigning a RECEPTIONIST to a second branch with HTTP 422 DomainException")
    void shouldRejectAssigningReceptionistToMultipleBranches() {
        String receptionistId = UUID.randomUUID().toString();
        String sedeCentroId = UUID.randomUUID().toString();
        String sedeNorteId = UUID.randomUUID().toString();
        when(staffAssignmentRepository.findAllByTenantSlugAndStaffId("clinica-irma", receptionistId))
                .thenReturn(List.of(new StaffBranchAssignmentEntity(
                        UUID.randomUUID(), "clinica-irma", receptionistId, "Laura Méndez", "RECEPTIONIST", sedeCentroId
                )));

        var command = new AssignStaffCommand("clinica-irma", receptionistId, "Laura Méndez", "RECEPTIONIST", sedeNorteId);

        assertThatThrownBy(() -> branchManagementService.assignStaffToBranch(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(domainEx.getMessage()).contains("Receptionists are strictly bound to a single branch");
                });

        verify(staffAssignmentRepository, never()).save(any());
    }
}
