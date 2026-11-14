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
public class BranchController {

    private final BranchManagementService branchService;

    public BranchController(BranchManagementService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<List<BranchResponse>> listBranches(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listBranches(tenantSlug));
    }

    @PostMapping
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
    public ResponseEntity<List<PhysicalRoomResponse>> listRooms(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant,
            @PathVariable UUID branchId
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listRooms(tenantSlug, branchId));
    }

    @PostMapping("/{branchId}/rooms")
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
    public ResponseEntity<List<StaffAssignmentResponse>> listStaffAssignments(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String headerTenant
    ) {
        String tenantSlug = resolveTenant(headerTenant);
        return ResponseEntity.ok(branchService.listStaffAssignments(tenantSlug));
    }

    @PostMapping("/staff-assignments")
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
