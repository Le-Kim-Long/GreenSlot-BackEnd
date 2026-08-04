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
    @JoinColumn(name = "location_id")
    private Location location;

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
}
