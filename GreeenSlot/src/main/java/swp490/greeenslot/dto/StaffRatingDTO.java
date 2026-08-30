package swp490.greeenslot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffRatingDTO {
    
    private Long id;
    
    private Long ratedStaffId;
    
    private String ratedStaffName;
    
    private Long ratedById;
    
    private String ratedByName;
    
    @NotNull(message = "Đánh giá sao không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer rating;
    
    private String comment;
    
    private String ratedAt;
}
