package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.StaffRating;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRatingRepository extends JpaRepository<StaffRating, Long> {
    
    List<StaffRating> findByRatedStaffId(Long staffId);
    
    Optional<StaffRating> findByRatedStaffIdAndRatedById(Long staffId, Long ratedBy);
    
    List<StaffRating> findByRatedById(Long ratedBy);
}
