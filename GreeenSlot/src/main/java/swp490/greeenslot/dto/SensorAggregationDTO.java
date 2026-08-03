package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorAggregationDTO {
    private LocalDateTime timestamp;
    private LocalDate date;
    private Integer hour;
    private Double avgValue;
    private Double minValue;
    private Double maxValue;
    private Long count;
}
