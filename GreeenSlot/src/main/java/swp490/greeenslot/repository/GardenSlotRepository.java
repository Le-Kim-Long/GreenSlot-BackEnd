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

    @Query("SELECT g FROM GardenSlot g WHERE g.status = :status AND (:locationId IS NULL OR g.pillar.location.id = :locationId)")
    List<GardenSlot> findByStatusAndLocationId(@Param("status") ESlotStatus status, @Param("locationId") Long locationId);

    @Query("SELECT g FROM GardenSlot g WHERE g.status = :status AND g.price >= :minPrice AND g.price <= :maxPrice")
    List<GardenSlot> findByStatusAndPriceRange(@Param("status") ESlotStatus status, @Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice);

    @Query("SELECT g FROM GardenSlot g WHERE g.status = :status AND g.pillar.location.id = :locationId AND g.price >= :minPrice AND g.price <= :maxPrice")
    List<GardenSlot> findByStatusAndLocationIdAndPriceRange(@Param("status") ESlotStatus status, @Param("locationId") Long locationId, @Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GardenSlot g WHERE g.id = :id")
    Optional<GardenSlot> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPillarId(Long pillarId);

    List<GardenSlot> findByPillarId(Long pillarId);
    
    Optional<GardenSlot> findBySlotNumber(String slotNumber);
}
