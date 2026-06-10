package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Mot gia tri cam bien trong payload Arduino.
 * THEM_CAM_BIEN_MOI: gui them object {"sensorType":"...", "value":...} trong mang readings.
 */
public class SensorReadingItemDTO {

    @NotBlank(message = "sensorType is required")
    @Schema(
            description = "Loai cam bien. Hien co: SOIL_MOISTURE, PH (hoac soil_moisture, ph)",
            example = "SOIL_MOISTURE"
    )
    private String sensorType;

    @NotNull(message = "value is required")
    @Schema(description = "Gia tri do duoc", example = "65.5")
    private Double value;

    @Schema(description = "Don vi (tuy chon). Bo trong de dung don vi mac dinh trong ESensorType", example = "%")
    private String unit;

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
