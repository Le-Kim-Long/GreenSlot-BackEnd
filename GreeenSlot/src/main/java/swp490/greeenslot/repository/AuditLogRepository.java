package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findAllByOrderByPerformedAtDesc();
}
