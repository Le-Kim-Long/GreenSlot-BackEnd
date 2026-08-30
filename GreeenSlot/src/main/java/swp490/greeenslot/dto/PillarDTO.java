package swp490.greeenslot.dto;

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
public class PillarDTO {
    private Long id;

    @NotBlank(message = "Mã trụ không được để trống")
    private String pillarCode;

    private String status; // ACTIVE, MAINTENANCE

    @NotNull(message = "Cơ sở không được để trống")
    @Positive(message = "Mã cơ sở không hợp lệ")
    private Long locationId;

    private String imageUrl;

    private String pillarType; // SMALL, MEDIUM, LARGE
    private String pillarTypeName;
    private Integer capacityHoles;
    private BigDecimal price;
    private Double requiredArea;
    private Long defaultTreeId;
    private String defaultTreeName;
    private BigDecimal defaultTreePrice;

    private Long slotId;
    private String slotNumber;

    public PillarDTO(Long id, String pillarCode, String status, Long locationId, String imageUrl) {
        this.id = id;
        this.pillarCode = pillarCode;
        this.status = status;
        this.locationId = locationId;
        this.imageUrl = imageUrl;
    }
}
