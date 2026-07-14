package swp490.greeenslot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ExtensionRequestDTO {
    @NotNull(message = "Rental ID is required")
    @Positive(message = "Rental ID must be positive")
    private Long rentalId;

    @Min(value = 1, message = "Duration must be at least 1 month")
    private int durationInMonths;

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }
}
