package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Entity
@Table(name = "trees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "tree_name", nullable = false)
    private String treeName;

    @Nationalized
    @Column(name = "scientific_name")
    private String scientificName;

    @Nationalized
    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "harvest_days", nullable = false)
    private Integer harvestDays;

    @Column(name = "min_rental_days", nullable = false)
    private Integer minRentalDays;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "price_small", precision = 12, scale = 2)
    private BigDecimal priceSmall;

    @Column(name = "price_medium", precision = 12, scale = 2)
    private BigDecimal priceMedium;

    @Column(name = "price_large", precision = 12, scale = 2)
    private BigDecimal priceLarge;

    public BigDecimal getEffectivePriceSmall() {
        if (priceSmall != null && priceSmall.compareTo(BigDecimal.ZERO) > 0) {
            return priceSmall;
        }
        return price != null ? price : BigDecimal.ZERO;
    }

    public BigDecimal getEffectivePriceMedium() {
        if (priceMedium != null && priceMedium.compareTo(BigDecimal.ZERO) > 0) {
            return priceMedium;
        }
        BigDecimal base = getEffectivePriceSmall();
        return base.multiply(BigDecimal.valueOf(1.5));
    }

    public BigDecimal getEffectivePriceLarge() {
        if (priceLarge != null && priceLarge.compareTo(BigDecimal.ZERO) > 0) {
            return priceLarge;
        }
        BigDecimal base = getEffectivePriceSmall();
        return base.multiply(BigDecimal.valueOf(2.0));
    }

    public BigDecimal getEffectivePriceForPillar(Pillar pillar) {
        if (pillar == null) {
            return getEffectivePriceSmall();
        }
        int holes = pillar.getEffectiveHoles();
        EPillarType type = pillar.getEffectivePillarType();
        if (holes >= 48 || type == EPillarType.LARGE) {
            return getEffectivePriceLarge();
        } else if (holes >= 36 || type == EPillarType.MEDIUM) {
            return getEffectivePriceMedium();
        } else {
            return getEffectivePriceSmall();
        }
    }


    @Column(name = "image_url")
    private String imageUrl;


    @Column(name = "soil_moisture_min")
    private Double soilMoistureMin;

    @Column(name = "soil_moisture_max")
    private Double soilMoistureMax;

    @Column(name = "light_min")
    private Double lightMin;

    @Column(name = "light_max")
    private Double lightMax;

    @Column(name = "ph_min")
    private Double phMin;

    @Column(name = "ph_max")
    private Double phMax;

    @Column(name = "compensation_percentage")
    private Integer compensationPercentage;

    @Nationalized
    @Column(name = "care_instructions", length = 4000)
    private String careInstructions;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
