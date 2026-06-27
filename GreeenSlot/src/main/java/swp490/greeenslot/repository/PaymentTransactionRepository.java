package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByVnpTxnRef(String vnpTxnRef);
    List<PaymentTransaction> findByRentalIdOrderByPaymentDateDesc(Long rentalId);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.rental.gardenSlot.id = :slotId AND t.status = 'PENDING' AND t.paymentDate > :cutoff")
    List<PaymentTransaction> findRecentPendingTransactions(@Param("slotId") Long slotId, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'SUCCESS' AND t.paymentDate BETWEEN :start AND :end ORDER BY t.paymentDate ASC")
    List<PaymentTransaction> findSuccessfulTransactionsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    boolean existsByRentalGardenSlotId(Long slotId);
}
