package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorAggregateDTO {
    private LocalDateTime timestamp;
    private String sensorType;
    private Double avgValue;
    private Double minValue;
    private Double maxValue;
    private Long readingCount;
}

