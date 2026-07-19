package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDTO {
    private Long locationId;
    private String locationName;
    private Long totalSlots;
    private Long availableSlots;
    private Long activeRentals;
    private Long pendingAlerts;
    private BigDecimal totalRevenue;
    private List<AlertDTO> recentAlerts;
    private List<ActiveRentalDTO> activeRentalsList;
}
