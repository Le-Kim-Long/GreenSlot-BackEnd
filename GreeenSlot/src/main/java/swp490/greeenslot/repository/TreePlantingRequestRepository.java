package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.EPlantingRequestStatus;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.TreePlantingRequest;
import swp490.greeenslot.entity.User;

import java.util.List;

@Repository
public interface TreePlantingRequestRepository extends JpaRepository<TreePlantingRequest, Long> {
    
    List<TreePlantingRequest> findByRequestedBy(User user);
    
    List<TreePlantingRequest> findByRental(SlotRental rental);
    
    List<TreePlantingRequest> findByStatus(EPlantingRequestStatus status);
    
    List<TreePlantingRequest> findByRequestedByAndStatus(User user, EPlantingRequestStatus status);
}
