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

    @NotNull(message = "Nhân viên không được để trống")
    private Long staffId;

    private String staffName;

    @NotNull(message = "Cơ sở không được để trống")
    private Long locationId;

    private String locationName;

    @NotNull(message = "Ngày trực không được để trống")
    @FutureOrPresent(message = "Ngày trực không được trong quá khứ")
    private LocalDate scheduleDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private Long slotId;
    private String slotNumber;

    private String notes;
    private Boolean isActive;
}
