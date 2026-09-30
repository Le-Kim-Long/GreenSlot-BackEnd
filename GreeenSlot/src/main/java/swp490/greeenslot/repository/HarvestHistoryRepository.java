package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.HarvestHistory;

import java.util.List;

@Repository
public interface HarvestHistoryRepository extends JpaRepository<HarvestHistory, Long> {

    List<HarvestHistory> findByCustomerIdOrderByHarvestedAtDesc(Long customerId);

    List<HarvestHistory> findByLocationIdOrderByHarvestedAtDesc(Long locationId);

    List<HarvestHistory> findAllByOrderByHarvestedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(h) FROM HarvestHistory h WHERE h.rentalId = :rentalId AND (h.pillarCodes = :pillarCode OR h.pillarCodes LIKE CONCAT('%, ', :pillarCode) OR h.pillarCodes LIKE CONCAT(:pillarCode, ',%') OR h.pillarCodes LIKE CONCAT('%, ', :pillarCode, ',%'))")
    long countHarvestsForPillar(@org.springframework.data.repository.query.Param("rentalId") Long rentalId, @org.springframework.data.repository.query.Param("pillarCode") String pillarCode);
}
