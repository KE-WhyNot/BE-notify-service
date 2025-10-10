package notify.domain.notify.domain.repository;


import notify.domain.notify.domain.entity.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP where n.id = :id and n.userId = :userId and n.readAt is null")
    int markRead(@Param("userId") Long userId, @Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId);

}