package pt.ipportalegre.estgd.ei.es.projectnotification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {
}