package swp490.greeenslot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload Arduino gui len server.
 *
 * Vi du JSON:
 * {
 *   "deviceId": "arduino-greenhouse-01",
 *   "readings": [
 *     { "sensorType": "SOIL_MOISTURE", "value": 65.5 },
 *     { "sensorType": "PH", "value": 6.8 }
 *   ]
 * }
 *
 * THEM_CAM_BIEN_MOI: them phan tu vao mang readings, khong can doi API endpoint.
 */
public class ArduinoSensorDataRequestDTO {

    @NotBlank(message = "deviceId is required")
    @Schema(description = "Ma dinh danh board Arduino", example = "arduino-greenhouse-01")
    private String deviceId;

    @NotEmpty(message = "readings must contain at least one sensor value")
    @Valid
    private List<SensorReadingItemDTO> readings;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<SensorReadingItemDTO> getReadings() {
        return readings;
    }

    public void setReadings(List<SensorReadingItemDTO> readings) {
        this.readings = readings;
    }
}
