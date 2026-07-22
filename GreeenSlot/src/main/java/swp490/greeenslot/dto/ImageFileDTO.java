package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageFileDTO {
    private Long id;
    private String fileName;
    private String storagePath;
    private String publicUrl;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String status;
    private Long uploadedBy;
    private String uploadedByUsername;
}
