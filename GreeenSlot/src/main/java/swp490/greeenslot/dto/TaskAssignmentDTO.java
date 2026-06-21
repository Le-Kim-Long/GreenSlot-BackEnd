package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentDTO {

    private Long taskId; // optional: if assigning an existing task

    @NotNull(message = "Staff ID is required")
    private Long staffId; // required: target staff user ID

    // Fields below are required ONLY if taskId is null (for creating a new task)
    private String taskName;
    private String description;
    private String taskType; // MAINTENANCE, CLEANING
    private Long targetSlotId;
}
