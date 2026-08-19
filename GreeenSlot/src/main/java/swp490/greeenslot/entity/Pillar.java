package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pillars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pillar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pillar_code", nullable = false, unique = true)
    private String pillarCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EPillarStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private GardenSlot gardenSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "pillar_type", length = 30)
    private EPillarType pillarType = EPillarType.MEDIUM;

    @Column(name = "capacity_holes")
    private Integer capacityHoles = 36;

    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal price = java.math.BigDecimal.valueOf(200000);

    @Column(name = "camera_stream_url")
    private String cameraStreamUrl;

    @Column(name = "camera_status")
    private String cameraStatus; // ONLINE, OFFLINE, MAINTENANCE

    @Column(name = "camera_last_heartbeat")
    private java.time.LocalDateTime cameraLastHeartbeat;

    @Column(name = "device_status")
    private String deviceStatus; // ONLINE, OFFLINE, MAINTENANCE

    @Column(name = "device_last_heartbeat")
    private java.time.LocalDateTime deviceLastHeartbeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_tree_id")
    private Tree defaultTree;

    @Column(name = "image_url")
    private String imageUrl;

    public EPillarType getEffectivePillarType() {
        return this.pillarType != null ? this.pillarType : EPillarType.MEDIUM;
    }

    public Integer getEffectiveHoles() {
        if (this.capacityHoles != null && this.capacityHoles > 0) {
            return this.capacityHoles;
        }
        return getEffectivePillarType().getDefaultHoles();
    }

    public java.math.BigDecimal getEffectivePrice() {
        if (this.price != null && this.price.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return this.price;
        }
        return getEffectivePillarType().getDefaultPrice();
    }

    public double getEffectiveArea() {
        return getEffectivePillarType().getMinRequiredArea();
    }
}
