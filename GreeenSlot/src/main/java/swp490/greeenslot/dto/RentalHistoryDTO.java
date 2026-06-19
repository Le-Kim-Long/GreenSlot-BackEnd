package swp490.greeenslot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RentalHistoryDTO {
    private Long rentalId;
    private String slotNumber;
    private String pillarCode;
    private String locationName;
    private String locationAddress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String rentalStatus;
    private List<PaymentTransactionInfo> transactions;

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

    public RentalHistoryDTO(Long rentalId, String slotNumber, String pillarCode, String locationName, String locationAddress,
                            LocalDateTime startTime, LocalDateTime endTime, String rentalStatus, List<PaymentTransactionInfo> transactions) {
        this.rentalId = rentalId;
        this.slotNumber = slotNumber;
        this.pillarCode = pillarCode;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rentalStatus = rentalStatus;
        this.transactions = transactions;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
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
}
