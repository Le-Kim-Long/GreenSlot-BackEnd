package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Append-only audit log of completed harvests. Fields are snapshotted (not live joins)
 * so history stays intact even if the rental/tree/location it referenced is later
 * modified or deleted — plain columns only, no @Enumerated, to avoid the stale
 * CHECK-constraint headaches hit elsewhere in this codebase.
 */
@Entity
@Table(name = "harvest_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HarvestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "location_id")
    private Long locationId;

    @Nationalized
    @Column(name = "location_name")
    private String locationName;

    @Column(name = "slot_id")
    private Long slotId;

    @Nationalized
    @Column(name = "slot_number")
    private String slotNumber;

    @Column(name = "tree_id")
    private Long treeId;

    @Nationalized
    @Column(name = "tree_name")
    private String treeName;

    @Column(name = "customer_id")
    private Long customerId;

    @Nationalized
    @Column(name = "customer_name")
    private String customerName;

    // Plain string ("SELF" | "STAFF"), not a Java enum on purpose — see class javadoc.
    @Column(name = "harvest_method", length = 20)
    private String harvestMethod;

    @Column(name = "staff_id")
    private Long staffId;

    @Nationalized
    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "planted_at")
    private LocalDateTime plantedAt;

    @Column(name = "harvested_at")
    private LocalDateTime harvestedAt;

    @Nationalized
    @Column(name = "pillar_codes", length = 255)
    private String pillarCodes;

    @Column(name = "harvest_days")
    private Integer harvestDays;

    @Column(name = "days_grown")
    private Integer daysGrown;

    @Column(name = "is_early_harvest")
    private Boolean isEarlyHarvest = false;
}
