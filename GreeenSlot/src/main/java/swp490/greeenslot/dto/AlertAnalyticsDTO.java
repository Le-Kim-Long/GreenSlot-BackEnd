package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertAnalyticsDTO {
    
    private Long totalAlerts;
    
    private Long pendingAlerts;
    
    private Long resolvedAlerts;
    
    private Long criticalAlerts;
    
    private Map<String, Long> alertsByType;
    
    private Map<String, Long> alertsBySensorType;
    
    private Long averageResolutionTimeMinutes;
    
    private Long mostCommonAlertFrequency;
    
    private String mostCommonAlertType;
}
