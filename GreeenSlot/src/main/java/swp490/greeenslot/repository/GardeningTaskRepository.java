package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.GardeningTask;

@Repository
public interface GardeningTaskRepository extends JpaRepository<GardeningTask, Long> {
    java.util.List<GardeningTask> findByAssignedStaffUsernameOrderByCreatedAtDesc(String username);
}
