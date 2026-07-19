package swp490.greeenslot.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.ESlotStatus;
import swp490.greeenslot.entity.GardenSlot;

import java.util.List;
import java.util.Optional;

@Repository
public interface GardenSlotRepository extends JpaRepository<GardenSlot, Long> {

    List<GardenSlot> findByPillarLocationIdAndStatus(Long locationId, ESlotStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GardenSlot g WHERE g.id = :id")
    Optional<GardenSlot> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPillarId(Long pillarId);

    List<GardenSlot> findByPillarId(Long pillarId);
    
    Optional<GardenSlot> findBySlotNumber(String slotNumber);
}
