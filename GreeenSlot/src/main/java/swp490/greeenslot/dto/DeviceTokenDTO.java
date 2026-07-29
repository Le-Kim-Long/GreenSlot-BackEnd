package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenDTO {
    @NotBlank(message = "Device token is required")
    private String deviceToken;
}
