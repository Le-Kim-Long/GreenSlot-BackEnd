package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.SlotRental;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotRentalRepository extends JpaRepository<SlotRental, Long> {
    List<SlotRental> findByUserUsernameOrderByStartTimeDesc(String username);

    @Query("SELECT r FROM SlotRental r WHERE r.gardenSlot.id = :slotId AND r.status = 'ACTIVE' AND r.endTime > :now")
    List<SlotRental> findActiveRentals(@Param("slotId") Long slotId, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r WHERE r.gardenSlot.id = :slotId AND r.user.username = :username AND r.status = 'ACTIVE' AND r.endTime > :now")
    java.util.Optional<SlotRental> findActiveRentalBySlotAndUser(@Param("slotId") Long slotId, @Param("username") String username, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM SlotRental r WHERE r.status = 'ACTIVE' ORDER BY r.startTime DESC")
    List<SlotRental> findAllActiveRentals();

    boolean existsByGardenSlotId(Long slotId);

    boolean existsByGardenSlotIdAndStatus(Long slotId, swp490.greeenslot.entity.ERentalStatus status);

    List<SlotRental> findByGardenSlotId(Long slotId);
}
