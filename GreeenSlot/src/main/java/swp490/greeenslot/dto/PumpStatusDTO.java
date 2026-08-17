package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PumpStatusDTO {

    @Schema(description = "Trạng thái của máy bơm", example = "ON", allowableValues = {"ON", "OFF"})
    private String status;

    @Schema(description = "Chế độ tự động tưới nước khi độ ẩm đất thấp", example = "true")
    private Boolean autoMode;

    @Schema(description = "Lý do kích hoạt gần nhất", example = "Tự động kích hoạt do độ ẩm đất 28.5% < ngưỡng min 40.0%")
    private String lastTriggerReason;

    @Schema(description = "Thời điểm kích hoạt gần nhất")
    private LocalDateTime lastTriggerTime;

    public PumpStatusDTO(String status) {
        this.status = status;
        this.autoMode = true;
    }
}