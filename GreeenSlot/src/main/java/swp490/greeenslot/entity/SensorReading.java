package swp490.greeenslot.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_device", columnList = "device_id"),
        @Index(name = "idx_sensor_type", columnList = "sensor_type"),
        @Index(name = "idx_sensor_recorded_at", columnList = "recorded_at")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ma dinh danh Arduino (vd: arduino-greenhouse-01) */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 50)
    private ESensorType sensorType;

    @Column(nullable = false)
    private Double value;

    @Column(length = 20)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public SensorReading() {
    }

    public SensorReading(String deviceId, ESensorType sensorType, Double value, String unit, Instant recordedAt) {
        this.deviceId = deviceId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
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
