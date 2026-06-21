package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "Service Type ID is required")
    private Long serviceTypeId;

    private String description;
}
