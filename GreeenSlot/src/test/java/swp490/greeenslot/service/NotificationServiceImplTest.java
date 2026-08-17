package swp490.greeenslot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp490.greeenslot.entity.Notification;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.NotificationRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.impl.NotificationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");

        testNotification = new Notification();
        testNotification.setId(100L);
        testNotification.setUserId(10L);
        testNotification.setTitle("Task Assigned");
        testNotification.setMessage("You have been assigned a task");
        testNotification.setType("TASK_ASSIGNMENT");
        testNotification.setReferenceId(50L);
        testNotification.setActionUrl("/dashboard/staff/tasks");
        testNotification.setRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully create notification with 6 arguments")
    void testCreateNotification_With6Args_Success() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(101L);
            return n;
        });

        Notification result = notificationService.createNotification(
                10L, "New Service Request", "Customer requested service", "SERVICE_REQUEST_CREATED", 500L, "/dashboard/manager/tasks"
        );

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(10L, result.getUserId());
        assertEquals("New Service Request", result.getTitle());
        assertEquals("Customer requested service", result.getMessage());
        assertEquals("SERVICE_REQUEST_CREATED", result.getType());
        assertEquals(500L, result.getReferenceId());
        assertEquals("/dashboard/manager/tasks", result.getActionUrl());
        assertFalse(result.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should successfully create notification with 4 arguments (overload)")
    void testCreateNotification_With4Args_Success() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(102L);
            return n;
        });

        Notification result = notificationService.createNotification(
                10L, "System Notice", "System maintenance tonight", "SYSTEM"
        );

        assertNotNull(result);
        assertEquals(102L, result.getId());
        assertEquals(10L, result.getUserId());
        assertNull(result.getReferenceId());
        assertNull(result.getActionUrl());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating notification for non-existing user")
    void testCreateNotification_ThrowsException_WhenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(999L, "Title", "Message", "SYSTEM")
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throttle IOT_ALERT notification when recent one exists within 15 minutes")
    void testCreateNotification_ThrottlesIoTAlert() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(notificationRepository.countRecentNotifications(eq(10L), eq("IOT_ALERT"), eq("IoT Alert"), any(LocalDateTime.class)))
                .thenReturn(1L);

        Notification result = notificationService.createNotification(
                10L, "IoT Alert", "Sensor reading out of range", "IOT_ALERT", 12L, "/dashboard/customer/iot"
        );

        assertNull(result);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get notifications for user ordered by creation date descending")
    void testGetUserNotifications_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(testNotification));

        List<Notification> results = notificationService.getUserNotifications("testuser");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getId());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("Should mark single notification as read")
    void testMarkAsRead_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markAsRead(100L, "testuser");

        assertNotNull(result);
        assertTrue(result.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when marking notification that does not belong to user")
    void testMarkAsRead_AccessDenied_WhenUserMismatch() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("otheruser");

        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));

        assertThrows(IllegalArgumentException.class, () ->
                notificationService.markAsRead(100L, "otheruser")
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get unread count by username and userId")
    void testGetUnreadCount_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(5L);

        long countByUsername = notificationService.getUnreadCount("testuser");
        long countByUserId = notificationService.getUnreadCount(10L);

        assertEquals(5L, countByUsername);
        assertEquals(5L, countByUserId);
        verify(notificationRepository, times(2)).countByUserIdAndIsReadFalse(10L);
    }

    @Test
    @DisplayName("Should mark all notifications as read by username and userId")
    void testMarkAllAsRead_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(notificationRepository.markAllAsReadByUserId(10L)).thenReturn(3);

        int updatedCountByUsername = notificationService.markAllAsRead("testuser");
        int updatedCountByUserId = notificationService.markAllAsRead(10L);

        assertEquals(3, updatedCountByUsername);
        assertEquals(3, updatedCountByUserId);
        verify(notificationRepository, times(2)).markAllAsReadByUserId(10L);
    }
}
