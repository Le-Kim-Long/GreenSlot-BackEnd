package swp490.greeenslot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    private Double area; // m2

    private Integer maxPillars;

    private Long locationId;

    private String locationName;

    private Long pillarId; // Backward-compatible single ID

    private List<Long> pillarIds = new ArrayList<>();

    private List<String> pillarCodes = new ArrayList<>();

    private Integer totalHoles;

    private BigDecimal calculatedPillarsPrice;

    private Double requiredArea;

    private String imageUrl;

    public GardenSlotDTO(Long id, String slotNumber, String status, BigDecimal price, Long pillarId, String imageUrl) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.status = status;
        this.price = price;
        this.pillarId = pillarId;
        this.imageUrl = imageUrl;
    }
}
