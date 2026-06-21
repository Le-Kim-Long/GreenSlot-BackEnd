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
public class LocationDTO {
    private Long id;

    @NotBlank(message = "Location name is required")
    private String name;

    @NotBlank(message = "Location address is required")
    private String address;

    private String contactPhone;

    private String status;

    @NotNull(message = "Area is required")
    private Double area;
}
