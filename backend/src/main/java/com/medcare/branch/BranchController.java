package com.medcare.branch;

import com.medcare.branch.BranchManagementService.AssignStaffCommand;
import com.medcare.branch.BranchManagementService.BranchResponse;
import com.medcare.branch.BranchManagementService.BranchSchedulingMatrixResponse;
import com.medcare.branch.BranchManagementService.CreateBranchCommand;
import com.medcare.branch.BranchManagementService.CreateRoomCommand;
import com.medcare.branch.BranchManagementService.PhysicalRoomResponse;
import com.medcare.branch.BranchManagementService.ReserveRoomCommand;
import com.medcare.branch.BranchManagementService.RoomReservationResponse;
import com.medcare.branch.BranchManagementService.StaffAssignmentResponse;
import com.medcare.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches & Consultation Rooms")
public class BranchController {

    private final BranchManagementService branchService;

    public BranchController(BranchManagementService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @Operation(summary = "List all clinic branches (sedes) for the active brand tenant")
    public ResponseEntity<List<BranchResponse>> listBranches(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listBranches(tenantSlug));
    }

    @PostMapping
    @Operation(summary = "Register a new clinic branch (sede) with IANA timezone validation")
    public ResponseEntity<BranchResponse> createBranch(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @Valid @RequestBody CreateBranchRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        BranchResponse response = branchService.registerBranch(new CreateBranchCommand(
                tenantSlug,
                request.name(),
                request.address(),
                request.timezone(),
                request.contactPhone()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{branchId}/rooms")
    @Operation(summary = "List physical consultation rooms (consultorios) for a branch")
    public ResponseEntity<List<PhysicalRoomResponse>> listRooms(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable UUID branchId
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listRooms(tenantSlug, branchId));
    }

    @PostMapping("/{branchId}/rooms")
    @Operation(summary = "Create a physical consultation room with strict concurrency capacity = 1")
    public ResponseEntity<PhysicalRoomResponse> createPhysicalRoom(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        int requestedCapacity = request.concurrencyCapacity() != null ? request.concurrencyCapacity() : 1;
        PhysicalRoomResponse response = branchService.registerPhysicalRoom(new CreateRoomCommand(
                tenantSlug,
                branchId,
                request.name(),
                request.roomType(),
                requestedCapacity
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{branchId}/rooms/{roomId}/reservations")
    @Operation(summary = "Reserve a physical room time slot (enforcing PostgreSQL btree_gist overlap exclusion)")
    public ResponseEntity<RoomReservationResponse> reserveRoom(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable UUID branchId,
            @PathVariable UUID roomId,
            @Valid @RequestBody ReserveRoomRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        RoomReservationResponse response = branchService.reserveRoomSlot(new ReserveRoomCommand(
                tenantSlug,
                branchId,
                roomId,
                request.doctorId(),
                request.startTime(),
                request.endTime()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/staff-assignments")
    @Operation(summary = "List all doctor and receptionist branch assignments for the active tenant")
    public ResponseEntity<List<StaffAssignmentResponse>> listStaffAssignments(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listStaffAssignments(tenantSlug));
    }

    @PostMapping("/staff-assignments")
    @Operation(summary = "Assign a doctor (multi-branch allowed) or receptionist (single-branch bound) to a branch")
    public ResponseEntity<StaffAssignmentResponse> assignStaff(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @Valid @RequestBody AssignStaffRequest request
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        StaffAssignmentResponse response = branchService.assignStaffToBranch(new AssignStaffCommand(
                tenantSlug,
                request.staffId(),
                request.staffName(),
                request.role(),
                request.branchId()
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{branchId}/scheduling-matrix")
    @Operation(summary = "Get the branch scheduling matrix of assigned doctors and receptionists")
    public ResponseEntity<BranchSchedulingMatrixResponse> getSchedulingMatrix(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable String branchId
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.getBranchSchedulingMatrix(tenantSlug, branchId));
    }

    private String resolveTenant(String headerTenant) {
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant.trim().toLowerCase();
        }
        return TenantContext.getCurrentTenantOrDefault("clinica-irma");
    }

    public record CreateBranchRequest(
            @NotBlank String name,
            @NotBlank String address,
            @NotBlank String timezone,
            @NotBlank String contactPhone
    ) {}

    public record CreateRoomRequest(
            @NotBlank String name,
            @NotBlank String roomType,
            Integer concurrencyCapacity
    ) {}

    public record ReserveRoomRequest(
            @NotNull UUID doctorId,
            @NotNull Instant startTime,
            @NotNull Instant endTime
    ) {}

    public record AssignStaffRequest(
            @NotBlank String staffId,
            @NotBlank String staffName,
            @NotBlank String role,
            @NotBlank String branchId
    ) {}
}
