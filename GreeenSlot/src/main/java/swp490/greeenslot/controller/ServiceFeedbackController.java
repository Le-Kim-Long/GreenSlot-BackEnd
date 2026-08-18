package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.ServiceFeedbackDTO;
import swp490.greeenslot.entity.ServiceFeedback;
import swp490.greeenslot.service.ServiceFeedbackService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/service-feedback")
@Tag(name = "Service Feedback", description = "APIs for service feedback and ratings")
public class ServiceFeedbackController {

    @Autowired
    private ServiceFeedbackService serviceFeedbackService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit service feedback", description = "Allows customers to rate and provide feedback on completed gardening tasks")
    public ResponseEntity<ServiceFeedback> createFeedback(
            @Valid @RequestBody ServiceFeedbackDTO feedbackDTO,
            Principal principal) {
        
        ServiceFeedback feedback = serviceFeedbackService.createFeedback(feedbackDTO, principal.getName());
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER', 'ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get feedback for a task", description = "Retrieves all feedback for a specific gardening task")
    public ResponseEntity<List<ServiceFeedbackDTO>> getFeedbackByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(serviceFeedbackService.getFeedbackByTaskId(taskId));
    }

    @GetMapping("/my-feedback")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my feedback", description = "Retrieves all feedback submitted by the authenticated user")
    public ResponseEntity<List<ServiceFeedbackDTO>> getMyFeedback(Principal principal) {
        return ResponseEntity.ok(serviceFeedbackService.getMyFeedback(principal.getName()));
    }

    @GetMapping("/task/{taskId}/average-rating")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER', 'ROLE_GARDEN_STAFF')")
    @Operation(summary = "Get average rating for a task", description = "Returns the average rating for a specific task")
    public ResponseEntity<Map<String, Double>> getAverageRatingForTask(@PathVariable Long taskId) {
        Double avgRating = serviceFeedbackService.getAverageRatingForTask(taskId);
        return ResponseEntity.ok(Map.of("averageRating", avgRating));
    }

    @GetMapping("/staff/{staffId}/average-rating")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get average rating for staff", description = "Returns the average rating for all tasks completed by a staff member")
    public ResponseEntity<Map<String, Double>> getAverageRatingForStaff(@PathVariable Long staffId) {
        Double avgRating = serviceFeedbackService.getAverageRatingForStaff(staffId);
        return ResponseEntity.ok(Map.of("averageRating", avgRating));
    }
}
