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
public class TransactionDeclarationDTO {
    private Long id;
    private Long rentalId;
    private String slotNumber;
    private String customerUsername;
    private String customerName;
    private BigDecimal amount;
    private String transactionCode;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String status;
    private String locationName;
    private String pillarCode;
    private String description;
}
