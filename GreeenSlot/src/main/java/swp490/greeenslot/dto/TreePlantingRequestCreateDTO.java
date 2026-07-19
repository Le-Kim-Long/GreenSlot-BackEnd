package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreePlantingRequestCreateDTO {
    @NotNull(message = "Rental ID is required")
    private Long rentalId;
    
    @NotNull(message = "New Tree ID is required")
    private Long newTreeId;
    
    @Nationalized
    private String reason;
    
    @Nationalized
    private String notes;
}
