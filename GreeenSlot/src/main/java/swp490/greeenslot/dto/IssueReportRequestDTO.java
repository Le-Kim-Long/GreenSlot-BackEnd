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
public class IssueReportRequestDTO {

    @NotBlank(message = "Issue title cannot be empty or whitespace")
    private String issueTitle;

    @NotBlank(message = "Description cannot be empty or whitespace")
    private String description;

    private String evidenceImageUrl; // optional image evidence
}
