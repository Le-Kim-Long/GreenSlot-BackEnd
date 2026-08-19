package swp490.greeenslot.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class BookingRequestDTO {
    @NotNull(message = "Slot ID is required")
    @Positive(message = "Slot ID must be positive")
    private Long slotId;

    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 120, message = "Duration cannot exceed 120 months")
    private int durationInMonths;

    private LocalDateTime startTime;

    private Boolean isMobile;

    private Long treeId;

    private java.util.List<Long> pillarIds = new java.util.ArrayList<>();

    private Integer smallPillarsCount;
    private Integer mediumPillarsCount;
    private Integer largePillarsCount;

    public Integer getSmallPillarsCount() {
        return smallPillarsCount;
    }

    public void setSmallPillarsCount(Integer smallPillarsCount) {
        this.smallPillarsCount = smallPillarsCount;
    }

    public Integer getMediumPillarsCount() {
        return mediumPillarsCount;
    }

    public void setMediumPillarsCount(Integer mediumPillarsCount) {
        this.mediumPillarsCount = mediumPillarsCount;
    }

    public Integer getLargePillarsCount() {
        return largePillarsCount;
    }

    public void setLargePillarsCount(Integer largePillarsCount) {
        this.largePillarsCount = largePillarsCount;
    }

    public java.util.List<Long> getPillarIds() {
        return pillarIds;
    }

    public void setPillarIds(java.util.List<Long> pillarIds) {
        this.pillarIds = pillarIds;
    }

    public Long getTreeId() {
        return treeId;
    }

    public void setTreeId(Long treeId) {
        this.treeId = treeId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Boolean getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(Boolean isMobile) {
        this.isMobile = isMobile;
    }
}
