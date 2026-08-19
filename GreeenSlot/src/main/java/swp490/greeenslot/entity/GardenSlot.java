package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "garden_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GardenSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_number", nullable = false, unique = true)
    private String slotNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ESlotStatus status;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "area")
    private Double area;

    @Column(name = "max_pillars")
    private Integer maxPillars;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "gardenSlot", fetch = FetchType.LAZY)
    private List<Pillar> pillars = new ArrayList<>();

    public Integer calculateMaxPillars() {
        if (this.area == null || this.area <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(this.area / 1.5));
    }

    public Pillar getPillar() {
        return (pillars != null && !pillars.isEmpty()) ? pillars.get(0) : null;
    }

    public void setPillar(Pillar pillar) {
        if (this.pillars == null) {
            this.pillars = new ArrayList<>();
        }
        if (pillar != null) {
            if (!this.pillars.contains(pillar)) {
                this.pillars.add(pillar);
            }
            pillar.setGardenSlot(this);
            if (this.location != null && pillar.getLocation() == null) {
                pillar.setLocation(this.location);
            }
        }
    }

    public void addPillar(Pillar pillar) {
        if (this.pillars == null) {
            this.pillars = new ArrayList<>();
        }
        if (pillar != null) {
            this.pillars.add(pillar);
            pillar.setGardenSlot(this);
            if (this.location != null && pillar.getLocation() == null) {
                pillar.setLocation(this.location);
            }
        }
    }
}
