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
public class ServiceCategoryDTO {
    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống")
    private String categoryName;

    private String description;
}
