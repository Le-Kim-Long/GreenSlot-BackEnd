package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    org.springframework.data.domain.Page<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(java.time.LocalDateTime start, java.time.LocalDateTime end, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<AuditLog> findAllByOrderByPerformedAtDesc(org.springframework.data.domain.Pageable pageable);
}
