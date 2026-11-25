package pt.ipportalegre.estgd.ei.es.projectnotification.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationPriority;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;

import java.util.Optional;
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

    @Query("""
        select n.id from Notification n
        where n.status = :status
          and (n.nextAttemptAt is null or n.nextAttemptAt <= :now)
          and (n.claimUntil is null or n.claimUntil <= :now)
        order by case
            when n.priority = :high then 0
            when coalesce(n.nextAttemptAt, n.createdAt) <= :tenMinutesAgo then 0
            when n.priority = :normal then 1
            when coalesce(n.nextAttemptAt, n.createdAt) <= :fiveMinutesAgo then 1
            else 2
        end,
        coalesce(n.nextAttemptAt, n.createdAt) asc,
        n.id asc
        """)
    List<Long> findClaimCandidates(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            @Param("high") NotificationPriority high,
            @Param("normal") NotificationPriority normal,
            @Param("fiveMinutesAgo") LocalDateTime fiveMinutesAgo,
            @Param("tenMinutesAgo") LocalDateTime tenMinutesAgo,
            Pageable page);

    List<Notification> findByStatusAndSentAtAfterOrderBySentAtAsc(NotificationStatus status,
                                                                  LocalDateTime since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notification> findLockedById(Long id);
}
