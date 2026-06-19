package swp490.greeenslot.dto;

public class BookingResponseDTO {
    private Long rentalId;
    private String paymentUrl;
    private String vnpTxnRef;

    public BookingResponseDTO() {
    }

    public BookingResponseDTO(Long rentalId, String paymentUrl, String vnpTxnRef) {
        this.rentalId = rentalId;
        this.paymentUrl = paymentUrl;
        this.vnpTxnRef = vnpTxnRef;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getVnpTxnRef() {
        return vnpTxnRef;
    }

    public void setVnpTxnRef(String vnpTxnRef) {
        this.vnpTxnRef = vnpTxnRef;
    }
}
