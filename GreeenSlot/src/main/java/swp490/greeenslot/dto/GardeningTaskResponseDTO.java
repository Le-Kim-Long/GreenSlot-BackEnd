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
public class GardeningTaskResponseDTO {
    private Long id;
    private String taskName;
    private String description;
    private String status;
    private String evidenceImageUrl;
    private String taskType;
    private Long assignedStaffId;
    private String assignedStaffName;
    private Long targetSlotId;
    private String targetSlotNumber;
    private LocalDateTime createdAt;
}
