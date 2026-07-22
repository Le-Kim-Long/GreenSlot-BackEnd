package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponseDTO {
    private Long id;
    private String fileName;
    private String publicUrl;
    private String contentType;
    private Long fileSize;
    private String message;
    private String uploadType; // IMAGE, EVIDENCE, AVATAR, VIDEO
}
