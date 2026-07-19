package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.AlertProcessingLog;
import swp490.greeenslot.entity.EAlertProcessingStatus;

import java.util.List;

@Repository
public interface AlertProcessingLogRepository extends JpaRepository<AlertProcessingLog, Long> {
    
    List<AlertProcessingLog> findByAlert(Alert alert);
    
    List<AlertProcessingLog> findByStatus(EAlertProcessingStatus status);
}
