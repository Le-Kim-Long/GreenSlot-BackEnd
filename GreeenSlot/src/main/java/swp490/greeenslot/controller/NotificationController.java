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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
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

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count for authenticated user",
            description = "Returns the total number of unread notifications for the logged in user.")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(principal.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as read",
            description = "Updates the isRead status of the specified notification to true.")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id, Principal principal) {
        Notification notification = notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(NotificationResponseDTO.fromEntity(notification));
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read",
            description = "Updates the isRead status of all notifications for the authenticated user to true.")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        int updatedCount = notificationService.markAllAsRead(principal.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        response.put("updatedCount", updatedCount);
        return ResponseEntity.ok(response);
    }
}
