package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "gardening_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GardeningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Nationalized
    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ETaskStatus status;

    @Column(name = "evidence_image_url")
    private String evidenceImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 30)
    private ETaskType taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_slot_id")
    private GardenSlot targetSlot;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
