package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.ServiceFeedback;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedback, Long> {
    
    List<ServiceFeedback> findByGardeningTaskId(Long taskId);
    
    Optional<ServiceFeedback> findByGardeningTaskIdAndUserId(Long taskId, Long userId);
    
    List<ServiceFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ServiceFeedback> findByUserIdAndRatingGreaterThanEqual(Long userId, Integer minRating);
}
