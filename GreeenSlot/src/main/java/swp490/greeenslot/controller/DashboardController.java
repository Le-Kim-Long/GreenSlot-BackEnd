package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.ActiveRentalDTO;
import swp490.greeenslot.dto.AlertDTO;
import swp490.greeenslot.dto.DashboardMetricsDTO;
import swp490.greeenslot.service.AlertService;
import swp490.greeenslot.service.BusinessManagementService;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "APIs for dashboard metrics and analytics by location")
public class DashboardController {

    @Autowired
    private BusinessManagementService businessManagementService;

    @Autowired
    private AlertService alertService;

    @GetMapping("/public")
    public String publicAccess() {
        return "Trang cong khai - tat ca deu truy cap duoc.";
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public String customerAccess() {
        return "Trang danh rieng cho Customer.";
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    public String staffAccess() {
        return "Trang danh rieng cho nhan vien (Admin, Manager, Garden Staff).";
    }

    @GetMapping("/metrics/{locationId}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get dashboard metrics for a specific location")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics(@PathVariable Long locationId) {
        DashboardMetricsDTO metrics = new DashboardMetricsDTO();
        
        String locationName = businessManagementService.getLocationById(locationId).getName();
        
        List<ActiveRentalDTO> activeRentals = businessManagementService.getActiveRentals();
        List<ActiveRentalDTO> locationRentals = activeRentals.stream()
                .filter(r -> r.getLocationName().equals(locationName))
                .toList();
        
        List<AlertDTO> pendingAlerts = alertService.getPendingAlerts();
        List<AlertDTO> locationAlerts = pendingAlerts.stream()
                .filter(a -> a.getPillarCode() != null)
                .filter(a -> {
                    swp490.greeenslot.dto.PillarDTO pillar = businessManagementService.getPillarById(a.getPillarId());
                    return pillar != null && pillar.getLocationId() != null && pillar.getLocationId().equals(locationId);
                })
                .toList();
        
        metrics.setLocationId(locationId);
        metrics.setLocationName(locationName);
        metrics.setActiveRentals((long) locationRentals.size());
        metrics.setPendingAlerts((long) locationAlerts.size());
        metrics.setActiveRentalsList(locationRentals);
        metrics.setRecentAlerts(locationAlerts);
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/{locationId}/revenue")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get revenue metrics for a specific location")
    public ResponseEntity<swp490.greeenslot.dto.RevenueAnalyticsResponseDTO> getLocationRevenue(
            @PathVariable Long locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        
        LocalDateTime startDateTime = java.time.LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime endDateTime = java.time.LocalDate.parse(endDate).atTime(java.time.LocalTime.MAX);
        
        swp490.greeenslot.dto.RevenueAnalyticsResponseDTO analytics = businessManagementService.getRevenueAnalytics(startDateTime, endDateTime);
        
        return ResponseEntity.ok(analytics);
    }
}
