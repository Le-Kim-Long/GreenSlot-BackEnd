package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.CustomerFeedback;
import swp490.greeenslot.entity.EFeedbackCategory;

import java.util.List;

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {
    
    List<CustomerFeedback> findByUserId(Long userId);
    
    List<CustomerFeedback> findByCategory(EFeedbackCategory category);
    
    List<CustomerFeedback> findByIsAnonymousFalseOrderByCreatedAtDesc();
}
