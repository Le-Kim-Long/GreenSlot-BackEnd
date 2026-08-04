package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.StaffRatingDTO;
import swp490.greeenslot.entity.StaffRating;
import swp490.greeenslot.service.StaffRatingService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/staff-ratings")
@Tag(name = "Staff Rating", description = "APIs for rating garden staff")
public class StaffRatingController {

    @Autowired
    private StaffRatingService staffRatingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rate a staff member", description = "Allows customers to rate garden staff members")
    public ResponseEntity<StaffRating> rateStaff(
            @Valid @RequestBody StaffRatingDTO ratingDTO,
            Principal principal) {
        StaffRating rating = staffRatingService.rateStaff(ratingDTO, principal.getName());
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get staff ratings", description = "Retrieves all ratings for a specific staff member")
    public ResponseEntity<List<StaffRatingDTO>> getStaffRatings(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffRatingService.getStaffRatings(staffId));
    }

    @GetMapping("/staff/{staffId}/average-rating")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get average rating for staff", description = "Returns the average rating for a staff member")
    public ResponseEntity<Map<String, Double>> getAverageRating(@PathVariable Long staffId) {
        Double avgRating = staffRatingService.getAverageRating(staffId);
        return ResponseEntity.ok(Map.of("averageRating", avgRating));
    }

    @GetMapping("/my-ratings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my ratings", description = "Retrieves all ratings submitted by the authenticated user")
    public ResponseEntity<List<StaffRatingDTO>> getMyRatings(Principal principal) {
        return ResponseEntity.ok(staffRatingService.getMyRatings(principal.getName()));
    }
}
