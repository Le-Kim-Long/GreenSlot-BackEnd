package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.NotificationPreferenceDTO;
import swp490.greeenslot.service.NotificationPreferenceService;

import java.security.Principal;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/notification-preferences")
@Tag(name = "Notification Preferences", description = "APIs for managing user notification preferences")
public class NotificationPreferenceController {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notification preferences", description = "Retrieves notification preferences for the authenticated user")
    public ResponseEntity<NotificationPreferenceDTO> getMyPreferences(Principal principal) {
        return ResponseEntity.ok(notificationPreferenceService.getPreferences(principal.getName()));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update notification preferences", description = "Updates notification preferences for the authenticated user")
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
            @Valid @RequestBody NotificationPreferenceDTO dto,
            Principal principal) {
        return ResponseEntity.ok(notificationPreferenceService.updatePreferences(dto, principal.getName()));
    }
}
