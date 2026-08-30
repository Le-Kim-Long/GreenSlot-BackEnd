package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskReviewRequestDTO {
    
    @NotBlank(message = "Hành động phê duyệt không được để trống (APPROVE hoặc REJECT)")
    private String action; // APPROVE, REJECT
    
    private String rejectionReason; // Required if action is REJECT
}
