package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.CustomerFeedbackDTO;
import swp490.greeenslot.entity.CustomerFeedback;
import swp490.greeenslot.service.CustomerFeedbackService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/customer-feedback")
@Tag(name = "Customer Feedback", description = "APIs for customer feedback collection")
public class CustomerFeedbackController {

    @Autowired
    private CustomerFeedbackService customerFeedbackService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit customer feedback", description = "Allows customers to submit general feedback about the service")
    public ResponseEntity<CustomerFeedback> submitFeedback(
            @Valid @RequestBody CustomerFeedbackDTO feedbackDTO,
            Principal principal) {
        CustomerFeedback feedback = customerFeedbackService.submitFeedback(feedbackDTO, principal.getName());
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/my-feedback")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my feedback", description = "Retrieves all feedback submitted by the authenticated user")
    public ResponseEntity<List<CustomerFeedbackDTO>> getMyFeedback(Principal principal) {
        return ResponseEntity.ok(customerFeedbackService.getMyFeedback(principal.getName()));
    }

    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get public feedback", description = "Retrieves all non-anonymous feedback for display")
    public ResponseEntity<List<CustomerFeedbackDTO>> getAllPublicFeedback() {
        return ResponseEntity.ok(customerFeedbackService.getAllPublicFeedback());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get feedback by category", description = "Retrieves feedback filtered by category")
    public ResponseEntity<List<CustomerFeedbackDTO>> getFeedbackByCategory(@PathVariable String category) {
        return ResponseEntity.ok(customerFeedbackService.getFeedbackByCategory(category));
    }
}
