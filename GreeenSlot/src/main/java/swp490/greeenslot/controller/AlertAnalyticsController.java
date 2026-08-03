package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.AlertAnalyticsDTO;
import swp490.greeenslot.service.AlertAnalyticsService;

import java.time.Instant;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/analytics/alerts")
@Tag(name = "Alert Analytics", description = "APIs for alert analytics and dashboard metrics")
public class AlertAnalyticsController {

    @Autowired
    private AlertAnalyticsService alertAnalyticsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get alert analytics", description = "Returns alert analytics metrics for a given date range")
    public ResponseEntity<AlertAnalyticsDTO> getAlertAnalytics(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        return ResponseEntity.ok(alertAnalyticsService.getAlertAnalytics(startDate, endDate));
    }
}
