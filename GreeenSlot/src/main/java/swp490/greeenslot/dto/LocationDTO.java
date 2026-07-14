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
public class LocationDTO {
    private Long id;

    @NotBlank(message = "Location name cannot be empty or whitespace")
    private String name;

    @NotBlank(message = "Location address cannot be empty or whitespace")
    private String address;

    private String contactPhone;

    private String status;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double area;
}
