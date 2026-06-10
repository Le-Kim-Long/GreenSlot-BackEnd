package swp490.greeenslot.dto;

import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.SensorReading;

import java.time.Instant;

public class SensorReadingResponseDTO {

    private Long id;
    private String deviceId;
    private ESensorType sensorType;
    private String sensorDescription;
    private Double value;
    private String unit;
    private Instant recordedAt;

    public static SensorReadingResponseDTO fromEntity(SensorReading reading) {
        SensorReadingResponseDTO dto = new SensorReadingResponseDTO();
        dto.setId(reading.getId());
        dto.setDeviceId(reading.getDeviceId());
        dto.setSensorType(reading.getSensorType());
        dto.setSensorDescription(reading.getSensorType().getDescription());
        dto.setValue(reading.getValue());
        dto.setUnit(reading.getUnit());
        dto.setRecordedAt(reading.getRecordedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public ESensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(ESensorType sensorType) {
        this.sensorType = sensorType;
    }

    public String getSensorDescription() {
        return sensorDescription;
    }

    public void setSensorDescription(String sensorDescription) {
        this.sensorDescription = sensorDescription;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
