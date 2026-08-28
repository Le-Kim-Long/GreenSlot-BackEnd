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
public class EligibleHarvestRentalDTO {
    private Long rentalId;
    private Long pillarId;
    private String pillarCode;
    private String slotNumber;
    private String treeName;
    private LocalDateTime plantedAt;
    private String pillarCodes;
    private Integer harvestDays;
    private Integer daysGrown;

    public EligibleHarvestRentalDTO(Long rentalId, String slotNumber, String treeName, LocalDateTime plantedAt, String pillarCodes, Integer harvestDays, Integer daysGrown) {
        this.rentalId = rentalId;
        this.slotNumber = slotNumber;
        this.treeName = treeName;
        this.plantedAt = plantedAt;
        this.pillarCodes = pillarCodes;
        this.pillarCode = pillarCodes;
        this.harvestDays = harvestDays;
        this.daysGrown = daysGrown;
    }
}
