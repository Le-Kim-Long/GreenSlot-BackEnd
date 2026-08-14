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

    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDateTime purchaseDate;

    @PastOrPresent(message = "Last maintenance date cannot be in the future")
    private LocalDateTime lastMaintenanceDate;

    private String imageUrl;
}
