package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationOperatingHoursDTO {
    private Long locationId;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String closedDays; // e.g., "SUNDAY,MONDAY"
}

