package pt.ipportalegre.estgd.ei.es.projectnotification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    /**
     * Notifications with the given status whose next attempt is due, oldest first.
     * A null nextAttemptAt means "never attempted / no delay", so it is always due.
     */
    @Query("""
            select n from Notification n
            where n.status = :status
              and (n.nextAttemptAt is null or n.nextAttemptAt <= :now)
            order by n.createdAt asc
            """)
    List<Notification> findDue(@Param("status") NotificationStatus status,
                               @Param("now") LocalDateTime now);
}
