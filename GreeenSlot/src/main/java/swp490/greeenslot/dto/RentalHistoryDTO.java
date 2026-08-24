package swp490.greeenslot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RentalHistoryDTO {
    private Long rentalId;
    private Long slotId;
    private String slotNumber;
    private String pillarCode;
    private List<String> pillarCodes = new java.util.ArrayList<>();
    private List<PillarInfo> pillars = new java.util.ArrayList<>();
    private String locationName;
    private String locationAddress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String rentalStatus;
    private List<PaymentTransactionInfo> transactions;
    private String treeName;
    private LocalDateTime harvestNotifiedAt;
    private String harvestDecision;
    private LocalDateTime plantedAt;
    private LocalDateTime expectedHarvestAt;
    private BigDecimal monthlyPrice;

    public static class PillarInfo {
        private Long id;
        private String pillarCode;
        private String status;
        private String cameraStreamUrl;
        private String cameraStatus;

        public PillarInfo() {}

        public PillarInfo(Long id, String pillarCode, String status, String cameraStreamUrl, String cameraStatus) {
            this.id = id;
            this.pillarCode = pillarCode;
            this.status = status;
            this.cameraStreamUrl = cameraStreamUrl;
            this.cameraStatus = cameraStatus;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPillarCode() { return pillarCode; }
        public void setPillarCode(String pillarCode) { this.pillarCode = pillarCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCameraStreamUrl() { return cameraStreamUrl; }
        public void setCameraStreamUrl(String cameraStreamUrl) { this.cameraStreamUrl = cameraStreamUrl; }
        public String getCameraStatus() { return cameraStatus; }
        public void setCameraStatus(String cameraStatus) { this.cameraStatus = cameraStatus; }
    }

    public static class PaymentTransactionInfo {
        private Long id;
        private BigDecimal amount;
        private String vnpTxnRef;
        private LocalDateTime paymentDate;
        private String status;

        public PaymentTransactionInfo() {
        }

        public PaymentTransactionInfo(Long id, BigDecimal amount, String vnpTxnRef, LocalDateTime paymentDate, String status) {
            this.id = id;
            this.amount = amount;
            this.vnpTxnRef = vnpTxnRef;
            this.paymentDate = paymentDate;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getVnpTxnRef() {
            return vnpTxnRef;
        }

        public void setVnpTxnRef(String vnpTxnRef) {
            this.vnpTxnRef = vnpTxnRef;
        }

        public LocalDateTime getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public RentalHistoryDTO() {
    }

    public RentalHistoryDTO(Long rentalId, Long slotId, String slotNumber, String pillarCode, String locationName, String locationAddress,
                            LocalDateTime startTime, LocalDateTime endTime, String rentalStatus, List<PaymentTransactionInfo> transactions,
                            String treeName, LocalDateTime harvestNotifiedAt, String harvestDecision,
                            LocalDateTime plantedAt, LocalDateTime expectedHarvestAt) {
        this.rentalId = rentalId;
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.pillarCode = pillarCode;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rentalStatus = rentalStatus;
        this.transactions = transactions;
        this.treeName = treeName;
        this.harvestNotifiedAt = harvestNotifiedAt;
        this.harvestDecision = harvestDecision;
        this.plantedAt = plantedAt;
        this.expectedHarvestAt = expectedHarvestAt;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getPillarCode() {
        return pillarCode;
    }

    public void setPillarCode(String pillarCode) {
        this.pillarCode = pillarCode;
    }

    public List<String> getPillarCodes() {
        return pillarCodes;
    }

    public void setPillarCodes(List<String> pillarCodes) {
        this.pillarCodes = pillarCodes;
    }

    public List<PillarInfo> getPillars() {
        return pillars;
    }

    public void setPillars(List<PillarInfo> pillars) {
        this.pillars = pillars;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getRentalStatus() {
        return rentalStatus;
    }

    public void setRentalStatus(String rentalStatus) {
        this.rentalStatus = rentalStatus;
    }

    public List<PaymentTransactionInfo> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PaymentTransactionInfo> transactions) {
        this.transactions = transactions;
    }

    public String getTreeName() {
        return treeName;
    }

    public void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    public LocalDateTime getHarvestNotifiedAt() {
        return harvestNotifiedAt;
    }

    public void setHarvestNotifiedAt(LocalDateTime harvestNotifiedAt) {
        this.harvestNotifiedAt = harvestNotifiedAt;
    }

    public String getHarvestDecision() {
        return harvestDecision;
    }

    public void setHarvestDecision(String harvestDecision) {
        this.harvestDecision = harvestDecision;
    }

    public LocalDateTime getPlantedAt() {
        return plantedAt;
    }

    public void setPlantedAt(LocalDateTime plantedAt) {
        this.plantedAt = plantedAt;
    }

    public LocalDateTime getExpectedHarvestAt() {
        return expectedHarvestAt;
    }

    public void setExpectedHarvestAt(LocalDateTime expectedHarvestAt) {
        this.expectedHarvestAt = expectedHarvestAt;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }
}
