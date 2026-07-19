package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.EAlertStatus;
import swp490.greeenslot.entity.Pillar;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(EAlertStatus status);
    
    List<Alert> findByPillar(Pillar pillar);
    
    List<Alert> findByPillarAndStatus(Pillar pillar, EAlertStatus status);
    
    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Alert> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    List<Alert> findByStatusOrderByCreatedAtDesc(EAlertStatus status);
}
