package swp490.greeenslot.service;

import swp490.greeenslot.dto.StaffRatingDTO;
import swp490.greeenslot.entity.StaffRating;

import java.util.List;

public interface StaffRatingService {
    
    StaffRating rateStaff(StaffRatingDTO ratingDTO, String username);
    
    List<StaffRatingDTO> getStaffRatings(Long staffId);
    
    Double getAverageRating(Long staffId);
    
    List<StaffRatingDTO> getMyRatings(String username);
}
