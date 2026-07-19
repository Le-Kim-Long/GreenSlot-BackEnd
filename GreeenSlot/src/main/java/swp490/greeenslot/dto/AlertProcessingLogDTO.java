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
public class AlertProcessingLogDTO {
    private Long id;
    private Long alertId;
    private Long processedById;
    private String processedByName;
    private String status;
    private String comment;
    private String evidenceImageUrl;
    private LocalDateTime processedAt;
}
