package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDTO {
    private Long id;
    private Long rentalId;
    private String slotNumber;
    private String customerUsername;
    private BigDecimal amount;
    private String vnpTxnRef;
    private LocalDateTime paymentDate;
    private String status;
}
