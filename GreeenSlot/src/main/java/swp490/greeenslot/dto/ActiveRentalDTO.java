package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRentalDTO {
    private Long rentalId;
    private String username;
    private String fullName;
    private String slotNumber;
    private String pillarCode;
    private String locationName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
