package swp490.greeenslot.service;

import swp490.greeenslot.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getUserNotifications(String username);
    Notification markAsRead(Long notificationId, String username);
    Notification createNotification(Long userId, String title, String message, String type);
    Notification createNotification(Long userId, String title, String message, String type, Long referenceId, String actionUrl);
    long getUnreadCount(String username);
    long getUnreadCount(Long userId);
    int markAllAsRead(String username);
    int markAllAsRead(Long userId);
}
