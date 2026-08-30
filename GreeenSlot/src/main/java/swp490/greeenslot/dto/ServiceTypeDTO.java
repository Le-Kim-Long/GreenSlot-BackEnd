package swp490.greeenslot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeDTO {
    private Long id;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String serviceName;

    private String description;

    @NotNull(message = "Đơn giá không được để trống")
    @Min(value = 1000, message = "Đơn giá dịch vụ phải tối thiểu 1.000 VNĐ")
    private BigDecimal price;

    @NotNull(message = "Danh mục dịch vụ không được để trống")
    @Positive(message = "Mã danh mục dịch vụ không hợp lệ")
    private Long categoryId;

    public ServiceTypeDTO(Long id, String serviceName, BigDecimal price, Long categoryId) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.categoryId = categoryId;
    }
}
