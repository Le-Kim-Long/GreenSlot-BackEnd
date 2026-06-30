package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.NotificationResponseDTO;
import swp490.greeenslot.entity.Notification;
import swp490.greeenslot.service.NotificationService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Endpoints for managing user alerts and notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications for authenticated user",
            description = "Fetches the current user's notifications, ordered by creation time descending.")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(Principal principal) {
        List<Notification> notifications = notificationService.getUserNotifications(principal.getName());
        List<NotificationResponseDTO> dtoList = notifications.stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as read",
            description = "Updates the isRead status of the specified notification to true.")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id, Principal principal) {
        Notification notification = notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(NotificationResponseDTO.fromEntity(notification));
    }
}
