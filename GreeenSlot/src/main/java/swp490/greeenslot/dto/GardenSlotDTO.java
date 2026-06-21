package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GardenSlotDTO {
    private Long id;

    @NotBlank(message = "Slot number is required")
    private String slotNumber;

    private String status; // AVAILABLE, RENTED, MAINTENANCE

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotNull(message = "Pillar ID is required")
    private Long pillarId;
}
