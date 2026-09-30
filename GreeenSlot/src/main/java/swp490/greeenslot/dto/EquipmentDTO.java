package swp490.greeenslot.dto;

import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDTO {
    private Long id;
    private String equipmentName;
    private String serialNumber;
    private String description;
    private String status;
    private Long pillarId;
    private String pillarCode;
    private Long locationId;
    private String locationName;

    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDateTime purchaseDate;

    @PastOrPresent(message = "Last maintenance date cannot be in the future")
    private LocalDateTime lastMaintenanceDate;

    private String imageUrl;
    private Integer quantity;

    public EquipmentDTO(Long id, String equipmentName, String serialNumber, String description, String status, Long pillarId, String pillarCode, Long locationId, String locationName, LocalDateTime purchaseDate, LocalDateTime lastMaintenanceDate, String imageUrl) {
        this.id = id;
        this.equipmentName = equipmentName;
        this.serialNumber = serialNumber;
        this.description = description;
        this.status = status;
        this.pillarId = pillarId;
        this.pillarCode = pillarCode;
        this.locationId = locationId;
        this.locationName = locationName;
        this.purchaseDate = purchaseDate;
        this.lastMaintenanceDate = lastMaintenanceDate;
        this.imageUrl = imageUrl;
        this.quantity = 1;
    }
}
