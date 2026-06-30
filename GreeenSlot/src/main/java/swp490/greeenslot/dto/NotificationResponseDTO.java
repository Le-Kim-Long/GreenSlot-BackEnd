package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swp490.greeenslot.entity.Notification;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDTO fromEntity(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
