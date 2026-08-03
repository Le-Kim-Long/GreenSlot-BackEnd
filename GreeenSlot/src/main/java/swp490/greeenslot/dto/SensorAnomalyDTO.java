package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorAnomalyDTO {
    
    private String deviceId;
    
    private String sensorType;
    
    private Double currentValue;
    
    private Double expectedValue;
    
    private Double deviationPercent;
    
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    private String description;
    
    private Instant detectedAt;
    
    private String recommendation;
}
