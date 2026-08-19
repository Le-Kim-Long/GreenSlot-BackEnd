package swp490.greeenslot.dto;

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

    private Long staffId; // required for assignStaffToTask, ignored for createTask

    // Fields below are required for createTask, ignored for assignStaffToTask
    private String taskName;
    private String description;
    private String taskType; // MAINTENANCE, CLEANING
    private Long targetSlotId;
    private String evidenceImageUrl; // optional reference/instruction image
}
