package swp490.greeenslot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sensor_thresholds", indexes = {
        @Index(name = "idx_threshold_device_sensor", columnList = "device_id, sensor_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã thiết bị không được để trống")
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId; // corresponds to pillar_code

    @NotBlank(message = "Loại cảm biến không được để trống")
    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType; // e.g. PH, SOIL_MOISTURE, etc.

    @NotNull(message = "Giá trị tối thiểu không được để trống")
    @Column(name = "min_value", nullable = false)
    private Double minValue;

    @NotNull(message = "Giá trị tối đa không được để trống")
    @Column(name = "max_value", nullable = false)
    private Double maxValue;
}
