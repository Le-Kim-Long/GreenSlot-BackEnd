package swp490.greeenslot.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleDTO {
    private Long id;

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    private String staffName;

    @NotNull(message = "Location ID is required")
    private Long locationId;

    private String locationName;

    @NotNull(message = "Schedule date is required")
    @FutureOrPresent(message = "Schedule date cannot be in the past")
    private LocalDate scheduleDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Long slotId;
    private String slotNumber;

    private String notes;
    private Boolean isActive;
}
