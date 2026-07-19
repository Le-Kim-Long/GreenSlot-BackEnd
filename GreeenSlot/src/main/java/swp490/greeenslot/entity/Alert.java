package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Nationalized
    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EAlertStatus status;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "actual_value")
    private Double actualValue;

    @Column(name = "sensor_type")
    private String sensorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pillar_id")
    private Pillar pillar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_slot_id")
    private GardenSlot gardenSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    private Tree tree;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
