package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.GardeningTask;
import swp490.greeenslot.service.GardeningTaskService;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
@Tag(name = "Gardening Task Workflow", description = "APIs for requesting services, assigning tasks, updating status, and reporting issues")
public class GardeningTaskController {

    @Autowired
    private GardeningTaskService gardeningTaskService;

    @PostMapping("/services/request")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Request an on-site gardening service", description = "Allows a customer to request a service for their active rented slot.")
    public ResponseEntity<GardeningTaskResponseDTO> requestService(
            @Valid @RequestBody ServiceRequestDTO request,
            Principal principal) {
        
        GardeningTask task = gardeningTaskService.requestService(request, principal.getName());
        return ResponseEntity.ok(mapToDTO(task));
    }

    @PostMapping("/tasks/assign")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Assign or create and assign a task to a staff", description = "Allows the manager to assign an existing task or create a new MAINTENANCE/CLEANING task and assign it to a staff member.")
    public ResponseEntity<GardeningTaskResponseDTO> assignTask(
            @Valid @RequestBody TaskAssignmentDTO request) {
        
        GardeningTask task = gardeningTaskService.assignTask(request);
        return ResponseEntity.ok(mapToDTO(task));
    }

    @GetMapping("/tasks/my-tasks")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get tasks assigned to current staff", description = "Retrieves tasks assigned to the authenticated garden staff, sorted by creation time descending.")
    public ResponseEntity<List<GardeningTaskResponseDTO>> getMyTasks(Principal principal) {
        List<GardeningTask> tasks = gardeningTaskService.getMyTasks(principal.getName());
        List<GardeningTaskResponseDTO> dtoList = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/tasks/{id}/status")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Update progress of a task", description = "Updates task status. Must include evidence image URL when marking status as COMPLETED.")
    public ResponseEntity<GardeningTaskResponseDTO> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateDTO request,
            Principal principal) {
        
        GardeningTask task = gardeningTaskService.updateTaskStatus(id, request, principal.getName());
        return ResponseEntity.ok(mapToDTO(task));
    }

    @PostMapping("/tasks/{id}/report-issue")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Report an issue related to a task", description = "Allows the staff working on a task to report a plant health or hardware/technical issue.")
    public ResponseEntity<GardeningTaskResponseDTO> reportIssue(
            @PathVariable Long id,
            @Valid @RequestBody IssueReportRequestDTO request,
            Principal principal) {
        
        GardeningTask task = gardeningTaskService.reportIssue(id, request, principal.getName());
        return ResponseEntity.ok(mapToDTO(task));
    }

    private GardeningTaskResponseDTO mapToDTO(GardeningTask task) {
        return new GardeningTaskResponseDTO(
                task.getId(),
                task.getTaskName(),
                task.getDescription(),
                task.getStatus().name(),
                task.getEvidenceImageUrl(),
                task.getTaskType().name(),
                task.getAssignedStaff() != null ? task.getAssignedStaff().getId() : null,
                task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : null,
                task.getTargetSlot() != null ? task.getTargetSlot().getId() : null,
                task.getTargetSlot() != null ? task.getTargetSlot().getSlotNumber() : null,
                task.getCreatedAt()
        );
    }
}
