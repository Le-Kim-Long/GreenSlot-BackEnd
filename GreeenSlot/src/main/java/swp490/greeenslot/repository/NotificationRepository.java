package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.type = :type AND n.title = :title AND n.createdAt > :cutoff")
    long countRecentNotifications(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("type") String type,
            @org.springframework.data.repository.query.Param("title") String title,
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff
    );
}
