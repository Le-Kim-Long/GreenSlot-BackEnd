package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.StaffRatingDTO;
import swp490.greeenslot.entity.StaffRating;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.StaffRatingRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.StaffRatingService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffRatingServiceImpl implements StaffRatingService {

    @Autowired
    private StaffRatingRepository staffRatingRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public StaffRating rateStaff(StaffRatingDTO ratingDTO, String username) {
        User ratedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        User ratedStaff = userRepository.findById(ratingDTO.getRatedStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + ratingDTO.getRatedStaffId()));

        // Check if user already rated this staff
        staffRatingRepository.findByRatedStaffIdAndRatedById(ratedStaff.getId(), ratedBy.getId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("You have already rated this staff member");
                });

        // Validate rating
        if (ratingDTO.getRating() < 1 || ratingDTO.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        StaffRating rating = new StaffRating();
        rating.setRatedStaff(ratedStaff);
        rating.setRatedBy(ratedBy);
        rating.setRating(ratingDTO.getRating());
        rating.setComment(ratingDTO.getComment());

        return staffRatingRepository.save(rating);
    }

    @Override
    public List<StaffRatingDTO> getStaffRatings(Long staffId) {
        List<StaffRating> ratings = staffRatingRepository.findByRatedStaffId(staffId);
        return ratings.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public Double getAverageRating(Long staffId) {
        List<StaffRating> ratings = staffRatingRepository.findByRatedStaffId(staffId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream().mapToInt(StaffRating::getRating).average().orElse(0.0);
    }

    @Override
    public List<StaffRatingDTO> getMyRatings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        List<StaffRating> ratings = staffRatingRepository.findByRatedById(user.getId());
        return ratings.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private StaffRatingDTO mapToDTO(StaffRating rating) {
        return new StaffRatingDTO(
                rating.getId(),
                rating.getRatedStaff() != null ? rating.getRatedStaff().getId() : null,
                rating.getRatedStaff() != null ? rating.getRatedStaff().getFullName() : null,
                rating.getRatedBy() != null ? rating.getRatedBy().getId() : null,
                rating.getRatedBy() != null ? rating.getRatedBy().getFullName() : null,
                rating.getRating(),
                rating.getComment(),
                rating.getRatedAt() != null ? rating.getRatedAt().toString() : null
        );
    }
}
