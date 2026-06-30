package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.GardeningTask;

@Repository
public interface GardeningTaskRepository extends JpaRepository<GardeningTask, Long> {
    java.util.List<GardeningTask> findByAssignedStaffUsernameOrderByCreatedAtDesc(String username);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t.assignedStaff FROM GardeningTask t WHERE t.targetSlot.id = :slotId AND t.assignedStaff IS NOT NULL")
    java.util.List<swp490.greeenslot.entity.User> findAssignedStaffBySlotId(@org.springframework.data.repository.query.Param("slotId") Long slotId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM GardeningTask t WHERE t.targetSlot.id = :slotId AND t.status = swp490.greeenslot.entity.ETaskStatus.PENDING")
    java.util.List<GardeningTask> findPendingTasksBySlotId(@org.springframework.data.repository.query.Param("slotId") Long slotId);
}
