package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import java.time.LocalTime;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false)
    private String name;

    @Nationalized
    private String address;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    private String status;

    private Double area;
    
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "open_time", columnDefinition = "TIME")
    private LocalTime openTime;

    @Column(name = "close_time", columnDefinition = "TIME")
    private LocalTime closeTime;

    @Column(name = "closed_days", length = 100)
    private String closedDays; // e.g., "SUNDAY,MONDAY"
}
