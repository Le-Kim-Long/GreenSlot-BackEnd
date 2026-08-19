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
    private String slotNumber;
    private String treeName;
    private LocalDateTime plantedAt;
}
