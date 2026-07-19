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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_tree_id")
    private Tree defaultTree;
}
