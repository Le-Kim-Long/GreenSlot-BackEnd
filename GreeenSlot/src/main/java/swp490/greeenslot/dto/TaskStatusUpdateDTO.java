package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateDTO {

    @NotBlank(message = "Status cannot be empty or whitespace")
    private String status; // PENDING, IN_PROGRESS, COMPLETED

    private String evidenceImageUrl; // required only if status is COMPLETED
}
