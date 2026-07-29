package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentDTO {

    @Schema(description = "ID of existing task. Leave null to create a new task.", example = "null", nullable = true)
    private Long taskId; // optional: if assigning an existing task

    @Schema(description = "ID of the staff to assign the task to", example = "4")
    @NotNull(message = "Staff ID is required")
    @Positive(message = "Staff ID must be positive")
    private Long staffId; // required: target staff user ID

    // Fields below are required ONLY if taskId is null (for creating a new task)
    @Schema(description = "Name of the task (required if taskId is null)", example = "Watering Plants")
    private String taskName;

    @Schema(description = "Detailed description of the task", example = "Water all plants in the slot and check for pests.")
    private String description;

    @Schema(description = "Type of task: MAINTENANCE or CLEANING (required if taskId is null)", example = "MAINTENANCE")
    private String taskType; // MAINTENANCE, CLEANING

    @Schema(description = "ID of the target garden slot (required if taskId is null)", example = "1")
    private Long targetSlotId;
}
