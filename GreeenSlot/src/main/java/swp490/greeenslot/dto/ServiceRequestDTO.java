package swp490.greeenslot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    @NotNull(message = "Ô vườn không được để trống")
    @Positive(message = "Mã ô vườn không hợp lệ")
    private Long slotId;

    @NotNull(message = "Loại dịch vụ không được để trống")
    @Positive(message = "Mã loại dịch vụ không hợp lệ")
    private Long serviceTypeId;

    private String description;
}
