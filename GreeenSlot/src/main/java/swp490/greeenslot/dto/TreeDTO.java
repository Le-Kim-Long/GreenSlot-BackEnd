package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreeDTO {
    private Long id;
    private String treeName;
    private String scientificName;
    private String description;
    private Integer harvestDays;
    private Integer minRentalDays;
    private BigDecimal price;
    private String imageUrl;
    private Double soilMoistureMin;
    private Double soilMoistureMax;
    private Double lightMin;
    private Double lightMax;
    private Double phMin;
    private Double phMax;
    private Integer compensationPercentage;
    private String careInstructions;
    private Boolean isActive;
}
