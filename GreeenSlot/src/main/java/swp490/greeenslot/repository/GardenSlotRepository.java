package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.GardenSlot;

@Repository
public interface GardenSlotRepository extends JpaRepository<GardenSlot, Long> {
}
