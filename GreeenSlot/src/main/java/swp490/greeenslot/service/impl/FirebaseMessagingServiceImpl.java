package swp490.greeenslot.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.FirebaseMessagingService;

import java.util.ArrayList;
import java.util.List;

@Service
public class FirebaseMessagingServiceImpl implements FirebaseMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public void sendPushNotification(Long userId, String title, String body) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getDeviceToken() == null || user.getDeviceToken().isBlank()) {
            logger.warn("Cannot send push notification: User {} has no device token", userId);
            return;
        }

        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(user.getDeviceToken())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Push notification sent successfully to user {}: {}", userId, response);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send push notification to user {}: {}", userId, e.getMessage());
        } catch (IllegalStateException e) {
            // Firebase is optional in local development. A missing service-account
            // must not roll back an otherwise successful payment transaction.
            logger.warn("Skipping push notification for user {} because Firebase is not initialized: {}", userId, e.getMessage());
        }
    }

    @Override
    public void sendPushNotificationToMultipleUsers(List<Long> userIds, String title, String body) {
        for (Long userId : userIds) {
            sendPushNotification(userId, title, body);
        }
    }

    @Override
    public void sendPushNotificationToLocation(Long locationId, String title, String body, String role) {
        List<User> users = userRepository.findByRoleNameAndLocation(
                swp490.greeenslot.entity.ERole.valueOf(role), locationId);
        
        List<Long> userIds = new ArrayList<>();
        for (User user : users) {
            if (user.getDeviceToken() != null && !user.getDeviceToken().isBlank()) {
                userIds.add(user.getId());
            }
        }
        
        if (!userIds.isEmpty()) {
            sendPushNotificationToMultipleUsers(userIds, title, body);
        } else {
            logger.warn("No users with device tokens found for location {} and role {}", locationId, role);
        }
    }
}
