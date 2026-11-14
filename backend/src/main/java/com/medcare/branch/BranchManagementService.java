package com.medcare.branch;

import com.medcare.shared.exception.DomainException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AHC-15 / AHC-16 / AHC-17] Domain and application service for clinic branches,
 * physical consultation rooms (capacity = 1), and multi-branch staff assignment rules.
 */
@Service
public class BranchManagementService {

    private final ClinicBranchRepository branchRepository;
    private final PhysicalRoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final StaffBranchAssignmentRepository staffAssignmentRepository;

    public BranchManagementService(
            ClinicBranchRepository branchRepository,
            PhysicalRoomRepository roomRepository,
            RoomReservationRepository reservationRepository,
            StaffBranchAssignmentRepository staffAssignmentRepository
    ) {
        this.branchRepository = branchRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    @Transactional
    public BranchResponse registerBranch(CreateBranchCommand command) {
        if (command.tenantSlug() == null || command.tenantSlug().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Missing Tenant", "Tenant context is required.");
        }
        if (command.timezone() == null || !ZoneId.getAvailableZoneIds().contains(command.timezone())) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Timezone",
                    "Invalid IANA timezone: " + command.timezone()
            );
        }

        ClinicBranchEntity saved = branchRepository.save(new ClinicBranchEntity(
                UUID.randomUUID(),
                command.tenantSlug(),
                command.name(),
                command.address(),
                command.timezone(),
                command.contactPhone(),
                true
        ));
        return toBranchResponse(saved);
    }

    @Transactional
    public PhysicalRoomResponse registerPhysicalRoom(CreateRoomCommand command) {
        if (command.requestedCapacity() != 1) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Room Capacity",
                    "Each physical consultation room must have a strict concurrency capacity of 1."
            );
        }
        if (!branchRepository.existsByIdAndTenantSlug(command.branchId(), command.tenantSlug())) {
            throw new DomainException(
                    HttpStatus.NOT_FOUND,
                    "Branch Not Found",
                    "Branch does not exist in active tenant."
            );
        }

        PhysicalRoomEntity saved = roomRepository.save(new PhysicalRoomEntity(
                UUID.randomUUID(),
                command.tenantSlug(),
                command.branchId(),
                command.name(),
                command.roomType(),
                1
        ));
        return new PhysicalRoomResponse(
                saved.getId(),
                saved.getTenantSlug(),
                saved.getBranchId(),
                saved.getName(),
                saved.getRoomType(),
                saved.getConcurrencyCapacity()
        );
    }

    @Transactional
    public RoomReservationResponse reserveRoomSlot(ReserveRoomCommand command) {
        if (reservationRepository.existsOverlappingReservation(
                command.roomId(), command.startTime(), command.endTime())) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Physical Room Slot Conflict",
                    "Physical room already has an active consultation overlapping the requested time range (capacity = 1)."
            );
        }

        RoomReservationEntity saved = reservationRepository.save(new RoomReservationEntity(
                UUID.randomUUID(),
                command.tenantSlug(),
                command.branchId(),
                command.roomId(),
                command.doctorId(),
                command.startTime(),
                command.endTime(),
                "SCHEDULED"
        ));
        return new RoomReservationResponse(
                saved.getId(),
                saved.getRoomId(),
                saved.getDoctorId(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus()
        );
    }

    @Transactional
    public StaffAssignmentResponse assignStaffToBranch(AssignStaffCommand command) {
        List<StaffBranchAssignmentEntity> existing = staffAssignmentRepository
                .findAllByTenantSlugAndStaffId(command.tenantSlug(), command.staffId());
        Set<String> assignedBranches = existing.stream()
                .map(StaffBranchAssignmentEntity::getBranchId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if ("RECEPTIONIST".equalsIgnoreCase(command.role())
                && !assignedBranches.isEmpty()
                && !assignedBranches.contains(command.branchId())) {
            throw new DomainException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Single-Branch Constraint Violation",
                    "Receptionists are strictly bound to a single branch and cannot be assigned to multiple locations."
            );
        }

        if (!assignedBranches.contains(command.branchId())) {
            staffAssignmentRepository.save(new StaffBranchAssignmentEntity(
                    UUID.randomUUID(),
                    command.tenantSlug(),
                    command.staffId(),
                    command.staffName(),
                    command.role().toUpperCase(),
                    command.branchId()
            ));
            assignedBranches.add(command.branchId());
        }

        return new StaffAssignmentResponse(
                command.staffId(),
                command.staffName(),
                command.role().toUpperCase(),
                List.copyOf(assignedBranches)
        );
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(String tenantSlug) {
        return branchRepository.findAllByTenantSlug(tenantSlug).stream()
                .map(this::toBranchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhysicalRoomResponse> listRooms(String tenantSlug, UUID branchId) {
        return roomRepository.findAllByTenantSlugAndBranchId(tenantSlug, branchId).stream()
                .map(saved -> new PhysicalRoomResponse(
                        saved.getId(),
                        saved.getTenantSlug(),
                        saved.getBranchId(),
                        saved.getName(),
                        saved.getRoomType(),
                        saved.getConcurrencyCapacity()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffAssignmentResponse> listStaffAssignments(String tenantSlug) {
        List<StaffBranchAssignmentEntity> entities = staffAssignmentRepository.findAllByTenantSlug(tenantSlug);
        java.util.Map<String, List<StaffBranchAssignmentEntity>> grouped = entities.stream()
                .collect(Collectors.groupingBy(
                        StaffBranchAssignmentEntity::getStaffId,
                         java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));
        return grouped.values().stream()
                .map(group -> {
                    StaffBranchAssignmentEntity first = group.get(0);
                    List<String> branches = group.stream()
                            .map(StaffBranchAssignmentEntity::getBranchId)
                            .distinct()
                            .toList();
                    return new StaffAssignmentResponse(
                            first.getStaffId(),
                            first.getStaffName(),
                            first.getRole(),
                            branches
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchSchedulingMatrixResponse getBranchSchedulingMatrix(String tenantSlug, String branchId) {
        List<ProviderAssignmentItem> providers = staffAssignmentRepository
                .findAllByTenantSlugAndBranchIdAndRoleIgnoreCase(tenantSlug, branchId, "DOCTOR")
                .stream()
                .map(a -> new ProviderAssignmentItem(a.getStaffId(), a.getStaffName(), a.getRole(), a.getBranchId()))
                .toList();
        return new BranchSchedulingMatrixResponse(branchId, providers);
    }

    private BranchResponse toBranchResponse(ClinicBranchEntity entity) {
        return new BranchResponse(
                entity.getId(),
                entity.getTenantSlug(),
                entity.getName(),
                entity.getAddress(),
                entity.getTimezone(),
                entity.getContactPhone(),
                entity.isActive()
        );
    }

    public record CreateBranchCommand(
            String tenantSlug,
            String name,
            String address,
            String timezone,
            String contactPhone
    ) {}

    public record BranchResponse(
            UUID id,
            String tenantSlug,
            String name,
            String address,
            String timezone,
            String contactPhone,
            boolean active
    ) {}

    public record CreateRoomCommand(
            String tenantSlug,
            UUID branchId,
            String name,
            String roomType,
            int requestedCapacity
    ) {}

    public record PhysicalRoomResponse(
            UUID id,
            String tenantSlug,
            UUID branchId,
            String name,
            String roomType,
            int concurrencyCapacity
    ) {}

    public record ReserveRoomCommand(
            String tenantSlug,
            UUID branchId,
            UUID roomId,
            UUID doctorId,
            Instant startTime,
            Instant endTime
    ) {}

    public record RoomReservationResponse(
            UUID id,
            UUID roomId,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String status
    ) {}

    public record AssignStaffCommand(
            String tenantSlug,
            String staffId,
            String staffName,
            String role,
            String branchId
    ) {}

    public record StaffAssignmentResponse(
            String staffId,
            String staffName,
            String role,
            List<String> assignedBranches
    ) {}

    public record ProviderAssignmentItem(
            String staffId,
            String staffName,
            String role,
            String branchId
    ) {}

    public record BranchSchedulingMatrixResponse(
            String branchId,
            List<ProviderAssignmentItem> providers
    ) {}
}
