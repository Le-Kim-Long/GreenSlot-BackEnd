package swp490.greeenslot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private SlotRental rental;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "vnp_txn_ref")
    private String vnpTxnRef;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EPaymentMethod paymentMethod;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EPaymentStatus status;
}
