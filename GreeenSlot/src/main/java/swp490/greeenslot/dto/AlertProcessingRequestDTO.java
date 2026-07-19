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
public class AlertProcessingRequestDTO {
    @NotNull(message = "Alert ID is required")
    private Long alertId;
    
    @NotNull(message = "Status is required")
    private String status;
    
    @Nationalized
    private String comment;
    
    private String evidenceImageUrl;
}
