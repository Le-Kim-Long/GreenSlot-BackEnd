package swp490.greeenslot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ExtensionRequestDTO {
    @NotNull(message = "Rental ID is required")
    @Positive(message = "Rental ID must be positive")
    private Long rentalId;

    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 120, message = "Duration cannot exceed 120 months")
    private int durationInMonths;

    private Boolean isMobile;

    private String mobileRedirectUrl;

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

    public Boolean getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(Boolean isMobile) {
        this.isMobile = isMobile;
    }

    public String getMobileRedirectUrl() {
        return mobileRedirectUrl;
    }

    public void setMobileRedirectUrl(String mobileRedirectUrl) {
        this.mobileRedirectUrl = mobileRedirectUrl;
    }

    private String redirectUrl;

    public String getRedirectUrl() {
        return redirectUrl != null ? redirectUrl : mobileRedirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}

