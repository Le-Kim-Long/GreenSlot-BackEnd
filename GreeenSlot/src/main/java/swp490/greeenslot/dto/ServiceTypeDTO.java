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

    @NotBlank(message = "Service name cannot be empty or whitespace")
    private String serviceName;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 1000, message = "Price/Amount must be at least 1000")
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    private Long categoryId;

    public ServiceTypeDTO(Long id, String serviceName, BigDecimal price, Long categoryId) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.categoryId = categoryId;
    }
}
