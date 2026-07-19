package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.AlertDTO;
import swp490.greeenslot.dto.AlertProcessingLogDTO;
import swp490.greeenslot.dto.AlertProcessingRequestDTO;
import swp490.greeenslot.service.AlertService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alert Management", description = "APIs for managing sensor alerts and processing logs")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get all alerts")
    public ResponseEntity<List<AlertDTO>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertDTO> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get alerts by status")
    public ResponseEntity<List<AlertDTO>> getAlertsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @GetMapping("/pillar/{pillarId}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get alerts by pillar")
    public ResponseEntity<List<AlertDTO>> getAlertsByPillar(@PathVariable Long pillarId) {
        return ResponseEntity.ok(alertService.getAlertsByPillar(pillarId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get pending alerts")
    public ResponseEntity<List<AlertDTO>> getPendingAlerts() {
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Process an alert")
    public ResponseEntity<AlertProcessingLogDTO> processAlert(
            @Valid @RequestBody AlertProcessingRequestDTO request,
            Principal principal) {
        return ResponseEntity.ok(alertService.processAlert(request, principal.getName()));
    }

    @GetMapping("/{alertId}/logs")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get alert processing logs")
    public ResponseEntity<List<AlertProcessingLogDTO>> getAlertProcessingLogs(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.getAlertProcessingLogs(alertId));
    }
}
