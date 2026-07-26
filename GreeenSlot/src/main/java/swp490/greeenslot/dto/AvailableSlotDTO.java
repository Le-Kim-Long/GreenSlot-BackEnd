package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDTO {
    private Long id;
    private String slotNumber;
    private String status;
    private BigDecimal price;
    private String imageUrl;
    private Long pillarId;
    private String pillarCode;
    private Long locationId;
    private String locationName;
    private String locationAddress;
    private String locationImageUrl;
    private String treeName;
    private String treeDescription;
    private LocalDateTime lastWatered;
    private LocalDateTime lastFertilized;
    private Double currentSoilMoisture;
    private Double currentPh;
    private Double currentLightIntensity;
    private String deviceStatus;
}
