package swp490.greeenslot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank(message = "Slot number cannot be empty or whitespace")
    private String slotNumber;

    private String status; // AVAILABLE, RENTED, MAINTENANCE

    @NotNull(message = "Price is required")
    @Min(value = 1000, message = "Price/Amount must be at least 1000")
    private BigDecimal price;

    @NotNull(message = "Pillar ID is required")
    @Positive(message = "Pillar ID must be positive")
    private Long pillarId;

    private String imageUrl;
}
