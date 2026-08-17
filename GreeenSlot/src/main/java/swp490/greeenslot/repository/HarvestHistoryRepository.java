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
}
