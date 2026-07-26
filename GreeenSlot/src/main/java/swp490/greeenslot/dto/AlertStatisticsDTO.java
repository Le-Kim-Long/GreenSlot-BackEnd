package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatisticsDTO {
    private Long locationId;
    private String locationName;
    private Long totalAlerts;
    private Long pendingAlerts;
    private Long inProgressAlerts;
    private Long resolvedAlerts;
    private Long failedAlerts;
    private Map<String, Long> alertsByType; // TEMPERATURE, HUMIDITY, LIGHT, WATER
    private Map<String, Long> alertsByStatus; // PENDING, IN_PROGRESS, RESOLVED, FAILED
}
