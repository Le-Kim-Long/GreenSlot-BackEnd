package swp490.greeenslot.dto;

import java.math.BigDecimal;

public class AvailableSlotResponseDTO {
    private Long id;
    private String slotNumber;
    private BigDecimal price;
    private String status;
    private String pillarCode;
    private String locationName;

    public AvailableSlotResponseDTO() {
    }

    public AvailableSlotResponseDTO(Long id, String slotNumber, BigDecimal price, String status, String pillarCode, String locationName) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.price = price;
        this.status = status;
        this.pillarCode = pillarCode;
        this.locationName = locationName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPillarCode() {
        return pillarCode;
    }

    public void setPillarCode(String pillarCode) {
        this.pillarCode = pillarCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
