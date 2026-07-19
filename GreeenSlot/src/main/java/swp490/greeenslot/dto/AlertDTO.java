package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {
    private Long id;
    private String alertType;
    private String description;
    private String status;
    private Double thresholdValue;
    private Double actualValue;
    private String sensorType;
    private Long pillarId;
    private String pillarCode;
    private Long gardenSlotId;
    private String slotNumber;
    private Long treeId;
    private String treeName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
