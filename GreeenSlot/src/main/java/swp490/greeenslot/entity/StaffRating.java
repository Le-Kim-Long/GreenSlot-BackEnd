package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_staff_id", nullable = false)
    private User ratedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_by_id", nullable = false)
    private User ratedBy;

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 2000)
    private String comment;

    @Column(name = "rated_at", updatable = false)
    private LocalDateTime ratedAt;

    @PrePersist
    protected void onCreate() {
        if (ratedAt == null) {
            ratedAt = LocalDateTime.now();
        }
    }
}
