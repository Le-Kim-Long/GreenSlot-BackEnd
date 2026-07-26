package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsDTO {
    // Revenue Metrics
    private BigDecimal totalRevenue;
    private BigDecimal revenueGrowth;
    private List<RevenueByLocationDTO> revenueByLocation;
    
    // Rental Metrics
    private Long totalRentals;
    private Long activeRentals;
    private Long completedRentals;
    private Double occupancyRate;
    
    // Service Quality Metrics
    private Long totalServicesCompleted;
    private Long pendingServices;
    private Double averageCompletionTime;
    private Double serviceSatisfactionRate;
    
    // Customer Metrics
    private Long totalCustomers;
    private Long activeCustomers;
    private Long newCustomers;
    
    // Alert Metrics
    private Long totalAlerts;
    private Long resolvedAlerts;
    private Long pendingAlerts;
    private Double alertResolutionRate;
    
    // System Performance
    private Double systemUptime;
    private Long activeIoTDevices;
    private Long offlineIoTDevices;
    
    // Location Performance
    private List<LocationPerformanceDTO> locationPerformance;
    
    // Time Period
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationPerformanceDTO {
        private Long locationId;
        private String locationName;
        private BigDecimal revenue;
        private Long activeRentals;
        private Long pendingAlerts;
        private Double satisfactionScore;
    }
}
