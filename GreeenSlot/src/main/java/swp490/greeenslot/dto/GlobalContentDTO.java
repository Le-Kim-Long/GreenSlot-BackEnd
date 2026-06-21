package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swp490.greeenslot.entity.EContentType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalContentDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Content type is required")
    private EContentType contentType;

    private Boolean active;
}
