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
public class TreePlantingRequestDTO {
    private Long id;
    private Long rentalId;
    private String slotNumber;
    private Long newTreeId;
    private String newTreeName;
    private Long requestedById;
    private String requestedByName;
    private String status;
    private String reason;
    private String notes;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private Long processedById;
    private String processedByName;
}
