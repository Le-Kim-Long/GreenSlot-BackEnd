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

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
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

    @PostMapping("/tasks/create")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Create a new task", description = "Allows the manager to create a new MAINTENANCE or CLEANING task. Task ID is auto-generated.")
    public ResponseEntity<GardeningTaskResponseDTO> createTask(
            @Valid @RequestBody TaskAssignmentDTO request) {
        
        GardeningTask task = gardeningTaskService.createTask(request);
        return ResponseEntity.ok(mapToDTO(task));
    }

    @RequestMapping(value = { "/tasks/{taskId}/assign" }, method = { RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST })
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Assign staff to existing task", description = "Allows Location Manager to assign a garden staff member to an existing task by ID.")
    public ResponseEntity<GardeningTaskResponseDTO> assignStaffToTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignmentDTO request) {
        GardeningTask task = gardeningTaskService.assignStaffToTask(taskId, request);
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

    @GetMapping("/tasks/available")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get unclaimed tasks at the staff's own location", description = "Tasks not yet assigned to anyone (e.g. auto-created HARVEST tasks) that any staff at the same location can self-claim.")
    public ResponseEntity<List<GardeningTaskResponseDTO>> getAvailableTasks(Principal principal) {
        List<GardeningTask> tasks = gardeningTaskService.getAvailableTasks(principal.getName());
        List<GardeningTaskResponseDTO> dtoList = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/tasks/{id}/claim")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Self-claim an unassigned task", description = "Allows a garden staff member to assign an unclaimed task to themselves, without needing a manager to assign it.")
    public ResponseEntity<GardeningTaskResponseDTO> claimTask(@PathVariable Long id, Principal principal) {
        GardeningTask task = gardeningTaskService.claimTask(id, principal.getName());
        return ResponseEntity.ok(mapToDTO(task));
    }

    @PostMapping("/tasks/{id}/notify-harvest")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Notify the customer that their crop is ready to harvest", description = "Sent by the staff member who claimed the HARVEST task; lets the customer choose to self-harvest or have staff do it.")
    public ResponseEntity<GardeningTaskResponseDTO> notifyHarvestChoice(@PathVariable Long id, Principal principal) {
        GardeningTask task = gardeningTaskService.notifyHarvestChoice(id, principal.getName());
        return ResponseEntity.ok(mapToDTO(task));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ROLE_LOCATION_MANAGER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Get all gardening tasks", description = "Retrieves all gardening tasks in the system, sorted by creation time descending.")
    public ResponseEntity<List<GardeningTaskResponseDTO>> getAllTasks() {
        List<GardeningTask> tasks = gardeningTaskService.getAllTasks();
        List<GardeningTaskResponseDTO> dtoList = tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PatchMapping("/tasks/{id}/status")
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

    @PostMapping("/tasks/{id}/review")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Review task evidence", description = "Allows a Location Manager to approve or reject a task submitted by staff.")
    public ResponseEntity<GardeningTaskResponseDTO> reviewTaskEvidence(
            @PathVariable Long id,
            @Valid @RequestBody TaskReviewRequestDTO request) {
        
        GardeningTask task = gardeningTaskService.reviewTaskEvidence(id, request);
        return ResponseEntity.ok(mapToDTO(task));
    }

    @PatchMapping("/tasks/{id}/evidence")
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Update task evidence image", description = "Update or re-upload evidence image for a task")
    public ResponseEntity<GardeningTaskResponseDTO> updateTaskEvidence(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String evidenceImageUrl = body.get("evidenceImageUrl");
        GardeningTask task = gardeningTaskService.updateEvidenceImage(id, evidenceImageUrl);
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
                task.getCreatedAt(),
                task.getRejectionReason()
        );
    }
}
