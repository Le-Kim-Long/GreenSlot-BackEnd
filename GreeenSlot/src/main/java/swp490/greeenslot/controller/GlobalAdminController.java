package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.service.AdminService;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Global Administration", description = "Endpoints for platform administrators, user access controls, audit logs, and global content")
public class GlobalAdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get paginated list of all users", description = "Retrieve a list of all users on the platform with pagination.")
    public ResponseEntity<Page<UserAdminDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PutMapping("/users/{id}/authorities")
    @Operation(summary = "Update user roles/authorities", description = "Modify or grant roles to a specific user (e.g., ROLE_LOCATION_MANAGER, ROLE_GARDEN_STAFF).")
    public ResponseEntity<UserAdminDTO> updateUserAuthorities(
            @PathVariable Long id,
            @Valid @RequestBody UserAuthorityUpdateDTO dto) {
        return ResponseEntity.ok(adminService.updateUserAuthorities(id, dto));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable user account status", description = "Block or unblock user account access to the platform.")
    public ResponseEntity<UserAdminDTO> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO dto) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, dto));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Retrieve system audit logs", description = "Filter and fetch system access, security, and database modification logs.")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(adminService.getAuditLogs(startDate, endDate));
    }

    @PostMapping("/global-content")
    @Operation(summary = "Create global content announcement or configuration", description = "Publish a new announcement or a system-wide configuration.")
    public ResponseEntity<GlobalContentDTO> createGlobalContent(
            @Valid @RequestBody GlobalContentDTO dto) {
        return ResponseEntity.ok(adminService.createContent(dto));
    }

    @PutMapping("/global-content/{id}")
    @Operation(summary = "Update global content announcement or configuration", description = "Modify an existing announcement or configuration.")
    public ResponseEntity<GlobalContentDTO> updateGlobalContent(
            @PathVariable Long id,
            @Valid @RequestBody GlobalContentDTO dto) {
        return ResponseEntity.ok(adminService.updateContent(id, dto));
    }

    @GetMapping("/global-content")
    @Operation(summary = "Retrieve all global content announcements and configurations", description = "Fetch a list of all active/inactive announcements and configurations.")
    public ResponseEntity<List<GlobalContentDTO>> getAllGlobalContent() {
        return ResponseEntity.ok(adminService.getAllContent());
    }
}
