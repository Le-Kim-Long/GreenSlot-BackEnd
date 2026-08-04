package swp490.greeenslot.service;

import java.util.List;

public interface FirebaseMessagingService {
    void sendPushNotification(Long userId, String title, String body);
    void sendPushNotificationToMultipleUsers(List<Long> userIds, String title, String body);
    void sendPushNotificationToLocation(Long locationId, String title, String body, String role);
}
