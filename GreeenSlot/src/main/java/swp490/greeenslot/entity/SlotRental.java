package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "slot_rentals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_slot_id")
    private GardenSlot gardenSlot;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERentalStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    private Tree tree;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ETreeStatus treeStatus;

    @Nationalized
    @Column(name = "tree_notes", length = 4000)
    private String treeNotes;

    @Column(name = "planted_at")
    private LocalDateTime plantedAt;

    @Column(name = "harvest_reminder_sent")
    private Boolean harvestReminderSent = false;

    @Column(name = "harvest_notified_at")
    private LocalDateTime harvestNotifiedAt;

    // Plain string ("SELF" | "STAFF"), not a Java enum on purpose — avoids the
    // legacy stale-CHECK-constraint issue hit earlier with task_type/status.
    @Column(name = "harvest_decision", length = 20)
    private String harvestDecision;
}
