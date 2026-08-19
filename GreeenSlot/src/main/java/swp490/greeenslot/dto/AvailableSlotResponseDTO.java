package swp490.greeenslot.dto;

import java.math.BigDecimal;

public class AvailableSlotResponseDTO {
    private Long id;
    private String slotNumber;
    private BigDecimal price;
    private String status;
    private String pillarCode;
    private java.util.List<String> pillarCodes = new java.util.ArrayList<>();
    private Integer pillarCount;
    private String locationName;
    private String imageUrl;
    private Long locationId;
    private String locationAddress;
    private Double area;
    private Integer maxPillars;
    private java.util.List<PillarDetailDTO> pillars = new java.util.ArrayList<>();
    private Integer totalHoles;
    private BigDecimal calculatedPillarsPrice;
    private BigDecimal calculatedTreesPrice;

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

    public AvailableSlotResponseDTO(Long id, String slotNumber, BigDecimal price, String status, String pillarCode, String locationName, String imageUrl) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.price = price;
        this.status = status;
        this.pillarCode = pillarCode;
        this.locationName = locationName;
        this.imageUrl = imageUrl;
    }

    public AvailableSlotResponseDTO(Long id, String slotNumber, BigDecimal price, String status, String pillarCode, String locationName, String imageUrl, Long locationId, String locationAddress) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.price = price;
        this.status = status;
        this.pillarCode = pillarCode;
        this.locationName = locationName;
        this.imageUrl = imageUrl;
        this.locationId = locationId;
        this.locationAddress = locationAddress;
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

    public java.util.List<String> getPillarCodes() {
        return pillarCodes;
    }

    public void setPillarCodes(java.util.List<String> pillarCodes) {
        this.pillarCodes = pillarCodes;
    }

    public Integer getPillarCount() {
        return pillarCount;
    }

    public void setPillarCount(Integer pillarCount) {
        this.pillarCount = pillarCount;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Integer getMaxPillars() {
        return maxPillars;
    }

    public void setMaxPillars(Integer maxPillars) {
        this.maxPillars = maxPillars;
    }

    public java.util.List<PillarDetailDTO> getPillars() {
        return pillars;
    }

    public void setPillars(java.util.List<PillarDetailDTO> pillars) {
        this.pillars = pillars;
    }

    public Integer getTotalHoles() {
        return totalHoles;
    }

    public void setTotalHoles(Integer totalHoles) {
        this.totalHoles = totalHoles;
    }

    public BigDecimal getCalculatedPillarsPrice() {
        return calculatedPillarsPrice;
    }

    public void setCalculatedPillarsPrice(BigDecimal calculatedPillarsPrice) {
        this.calculatedPillarsPrice = calculatedPillarsPrice;
    }

    public BigDecimal getCalculatedTreesPrice() {
        return calculatedTreesPrice;
    }

    public void setCalculatedTreesPrice(BigDecimal calculatedTreesPrice) {
        this.calculatedTreesPrice = calculatedTreesPrice;
    }
}
