package swp490.greeenslot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeviceTelemetryRequestDTO {

    @NotBlank(message = "device_id is required")
    @JsonProperty("device_id")
    private String deviceId;

    @NotBlank(message = "sensor_type is required")
    @JsonProperty("sensor_type")
    private String sensorType;

    @NotNull(message = "value is required")
    private Double value;

    private String unit;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @JsonProperty("deviceId")
    public void setDeviceIdCamel(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    @JsonProperty("sensorType")
    public void setSensorTypeCamel(String sensorType) {
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
