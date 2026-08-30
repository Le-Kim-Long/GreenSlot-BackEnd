package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer harvest decision request DTO")
public class HarvestDecisionRequestDTO {

    @NotBlank(message = "Lựa chọn phương thức thu hoạch không được để trống (SELF hoặc STAFF)")
    @Schema(description = "Harvest decision type: SELF (customer harvests) or STAFF (staff assists)", example = "SELF")
    private String decision;

    @Schema(description = "Optional delivery address if staff harvest and delivery is requested", example = "123 Green Street, Ward 1, Dist 3, HCMC")
    private String deliveryAddress;

    @Schema(description = "Optional customer notes or instructions", example = "Please harvest carefully in morning hours")
    private String notes;
}
