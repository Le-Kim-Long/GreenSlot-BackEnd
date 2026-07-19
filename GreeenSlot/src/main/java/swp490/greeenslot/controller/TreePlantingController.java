package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.TreePlantingRequestCreateDTO;
import swp490.greeenslot.dto.TreePlantingRequestDTO;
import swp490.greeenslot.service.TreePlantingService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/tree-planting")
@Tag(name = "Tree Planting Requests", description = "APIs for managing customer tree planting requests")
public class TreePlantingController {

    @Autowired
    private TreePlantingService treePlantingService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get all tree planting requests")
    public ResponseEntity<List<TreePlantingRequestDTO>> getAllRequests() {
        return ResponseEntity.ok(treePlantingService.getAllRequests());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get request by ID")
    public ResponseEntity<TreePlantingRequestDTO> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(treePlantingService.getRequestById(id));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get my tree planting requests")
    public ResponseEntity<List<TreePlantingRequestDTO>> getMyRequests(Principal principal) {
        return ResponseEntity.ok(treePlantingService.getRequestsByUser(principal.getName()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get pending requests")
    public ResponseEntity<List<TreePlantingRequestDTO>> getPendingRequests() {
        return ResponseEntity.ok(treePlantingService.getPendingRequests());
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Create a tree planting request")
    public ResponseEntity<TreePlantingRequestDTO> createRequest(
            @Valid @RequestBody TreePlantingRequestCreateDTO dto,
            Principal principal) {
        return ResponseEntity.ok(treePlantingService.createRequest(dto, principal.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Approve a tree planting request")
    public ResponseEntity<TreePlantingRequestDTO> approveRequest(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(treePlantingService.approveRequest(id, principal.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Reject a tree planting request")
    public ResponseEntity<TreePlantingRequestDTO> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) String reason,
            Principal principal) {
        return ResponseEntity.ok(treePlantingService.rejectRequest(id, reason, principal.getName()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Complete a tree planting request")
    public ResponseEntity<TreePlantingRequestDTO> completeRequest(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(treePlantingService.completeRequest(id, principal.getName()));
    }
}
