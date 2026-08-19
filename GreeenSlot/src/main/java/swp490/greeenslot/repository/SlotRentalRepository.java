package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotRentalRepository extends JpaRepository<SlotRental, Long> {

    List<SlotRental> findByTree(Tree tree);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SlotRental r WHERE r.id = :id")
    java.util.Optional<SlotRental> findByIdWithPessimisticLock(@Param("id") Long id);

    List<SlotRental> findByUserUsernameOrderByStartTimeDesc(String username);

    @Query("SELECT DISTINCT r FROM SlotRental r " +
           "JOIN FETCH r.gardenSlot s " +
           "JOIN FETCH s.location l " +
           "LEFT JOIN FETCH s.pillars p " +
           "WHERE r.user.username = :username " +
           "ORDER BY r.startTime DESC")
    List<SlotRental> findByUserUsernameWithSlotAndPillarAndLocation(@Param("username") String username);

    @Query("SELECT r FROM SlotRental r WHERE r.gardenSlot.id = :slotId AND r.status = 'ACTIVE' AND r.endTime > :now")
    List<SlotRental> findActiveRentals(@Param("slotId") Long slotId, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r WHERE r.gardenSlot.id = :slotId AND r.user.username = :username AND r.status = 'ACTIVE' AND r.endTime > :now")
    java.util.Optional<SlotRental> findActiveRentalBySlotAndUser(@Param("slotId") Long slotId, @Param("username") String username, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'ACTIVE' ORDER BY r.startTime DESC")
    List<SlotRental> findAllActiveRentals();

    boolean existsByGardenSlotId(Long slotId);

    boolean existsByGardenSlotIdAndStatus(Long slotId, swp490.greeenslot.entity.ERentalStatus status);

    List<SlotRental> findByGardenSlotId(Long slotId);

    @Query("SELECT COUNT(r) FROM SlotRental r WHERE r.gardenSlot.id = :slotId AND r.id != :rentalId AND (r.status = 'ACTIVE' OR r.status = 'PENDING')")
    long countOtherActiveOrPending(@Param("slotId") Long slotId, @Param("rentalId") Long rentalId);

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'PENDING' AND EXISTS (SELECT t FROM PaymentTransaction t WHERE t.rental.id = r.id AND t.status = 'PENDING' AND t.paymentDate < :cutoff)")
    List<SlotRental> findStalePendingRentals(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'ACTIVE' AND r.endTime < :now")
    List<SlotRental> findExpiredRentals(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'ACTIVE' AND r.tree IS NOT NULL AND r.plantedAt IS NOT NULL")
    List<SlotRental> findHarvestReminderCandidates();

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'ACTIVE' AND r.tree IS NOT NULL " +
           "AND r.gardenSlot.location.id = :locationId ORDER BY r.plantedAt ASC")
    List<SlotRental> findActiveWithTreeByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT DISTINCT p.id FROM SlotRental r JOIN r.rentedPillars p WHERE (r.status = 'ACTIVE' OR r.status = 'PENDING') AND (r.endTime IS NULL OR r.endTime > :now)")
    List<Long> findCurrentlyRentedPillarIds(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r JOIN r.rentedPillars p WHERE p.id IN :pillarIds AND (r.status = 'ACTIVE' OR r.status = 'PENDING') AND (r.endTime IS NULL OR r.endTime > :now)")
    List<SlotRental> findActiveRentalsByPillarIds(@Param("pillarIds") List<Long> pillarIds, @Param("now") LocalDateTime now);
}
