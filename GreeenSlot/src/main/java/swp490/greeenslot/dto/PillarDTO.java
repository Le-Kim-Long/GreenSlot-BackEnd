package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank(message = "Pillar code cannot be empty or whitespace")
    private String pillarCode;

    private String status; // ACTIVE, MAINTENANCE

    @NotNull(message = "Location ID is required")
    @Positive(message = "Location ID must be positive")
    private Long locationId;
}
