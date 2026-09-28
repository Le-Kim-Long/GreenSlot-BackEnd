package swp490.greeenslot.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddPillarsRequestDTO {
    @Min(value = 0, message = "Số lượng trụ nhỏ không được âm")
    private Integer smallCount = 0;

    @Min(value = 0, message = "Số lượng trụ vừa không được âm")
    private Integer mediumCount = 0;

    @Min(value = 0, message = "Số lượng trụ lớn không được âm")
    private Integer largeCount = 0;

    private Boolean isMobile = false;
    private String redirectUrl;
}
