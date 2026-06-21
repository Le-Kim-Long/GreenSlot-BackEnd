package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
