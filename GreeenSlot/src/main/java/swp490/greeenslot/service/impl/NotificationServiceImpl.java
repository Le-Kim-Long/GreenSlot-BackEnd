package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.Notification;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.NotificationRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        if (!notification.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied: Notification does not belong to you.");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String title, String message, String type) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Cannot create notification: User does not exist with ID: " + userId);
        }

        // Throttle IoT Alert notifications: do not create duplicates if one was created in the last 15 minutes
        if ("IOT_ALERT".equals(type)) {
            long count = notificationRepository.countRecentNotifications(userId, type, title, LocalDateTime.now().minusMinutes(15));
            if (count > 0) {
                return null;
            }
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }
}
