package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, unique = true)
    private String storagePath;

    @Column(nullable = false, length = 1000)
    private String publicUrl;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
}