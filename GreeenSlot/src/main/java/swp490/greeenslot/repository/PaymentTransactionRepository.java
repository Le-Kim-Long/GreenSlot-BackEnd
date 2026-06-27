package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentTransaction> findByVnpTxnRef(String vnpTxnRef);
    List<PaymentTransaction> findByRentalIdOrderByPaymentDateDesc(Long rentalId);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.rental.gardenSlot.id = :slotId AND t.status = 'PENDING' AND t.paymentDate > :cutoff")
    List<PaymentTransaction> findRecentPendingTransactions(@Param("slotId") Long slotId, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'SUCCESS' AND t.paymentDate BETWEEN :start AND :end ORDER BY t.paymentDate ASC")
    List<PaymentTransaction> findSuccessfulTransactionsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'PENDING' AND t.paymentDate < :cutoff")
    List<PaymentTransaction> findStalePendingTransactions(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByRentalGardenSlotId(Long slotId);
}
