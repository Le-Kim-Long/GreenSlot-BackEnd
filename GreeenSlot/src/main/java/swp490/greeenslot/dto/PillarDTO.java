package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PillarDTO {
    private Long id;

    @NotBlank(message = "Pillar code is required")
    private String pillarCode;

    private String status; // ACTIVE, MAINTENANCE

    @NotNull(message = "Location ID is required")
    private Long locationId;
}
