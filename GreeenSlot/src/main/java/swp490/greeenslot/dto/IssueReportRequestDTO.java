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

    @NotBlank(message = "Tiêu đề sự cố không được để trống")
    private String issueTitle;

    @NotBlank(message = "Mô tả sự cố không được để trống")
    private String description;

    private String evidenceImageUrl; // optional image evidence
}
