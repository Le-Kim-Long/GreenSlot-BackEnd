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
