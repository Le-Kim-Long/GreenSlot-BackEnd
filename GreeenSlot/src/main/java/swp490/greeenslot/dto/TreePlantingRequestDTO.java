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
    private Long locationId;
    private String locationName;
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
    private java.math.BigDecimal amount;
    private String paymentUrl;
    private Long targetPillarId;
    private String targetPillarCode;
    private Integer targetPillarHoles;
    private String targetPillarType;
    private Boolean isPaid = false;

    public TreePlantingRequestDTO(Long id, Long rentalId, String slotNumber, Long locationId,
                                String locationName, Long newTreeId, String newTreeName,
                                Long requestedById, String requestedByName, String status,
                                String reason, String notes, LocalDateTime requestedAt,
                                LocalDateTime processedAt, Long processedById, String processedByName,
                                java.math.BigDecimal amount, String paymentUrl, Long targetPillarId,
                                String targetPillarCode, Integer targetPillarHoles, String targetPillarType) {
        this(id, rentalId, slotNumber, locationId, locationName, newTreeId, newTreeName,
             requestedById, requestedByName, status, reason, notes, requestedAt,
             processedAt, processedById, processedByName, amount, paymentUrl,
             targetPillarId, targetPillarCode, targetPillarHoles, targetPillarType, false);
    }
}
