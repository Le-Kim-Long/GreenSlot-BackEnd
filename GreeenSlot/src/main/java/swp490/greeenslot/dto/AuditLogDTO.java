package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String performedBy;
    private LocalDateTime performedAt;
    private String details;
    private String ipAddress;
}
