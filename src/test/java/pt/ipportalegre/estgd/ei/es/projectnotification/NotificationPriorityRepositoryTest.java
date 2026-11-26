package pt.ipportalegre.estgd.ei.es.projectnotification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Notification;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationPriority;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationStatus;
import pt.ipportalegre.estgd.ei.es.projectnotification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationChannel.CONSOLE;
import static pt.ipportalegre.estgd.ei.es.projectnotification.entity.NotificationPriority.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3307/notificationPriorityTestDB?createDatabaseIfNotExist=true",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationPriorityRepositoryTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 11, 25, 16, 0);

    @Autowired
    private NotificationRepository repository;

    @Test
    void higherPriorityComesFirstEvenWhenItArrivedLater() {
        Notification low = save(LOW, NOW.minusMinutes(3));
        Notification normal = save(NORMAL, NOW.minusMinutes(2));
        Notification high = save(HIGH, NOW.minusMinutes(1));

        assertEquals(List.of(high.getId(), normal.getId(), low.getId()), candidates());
    }

    @Test
    void samePriorityUsesOldestFirstThenId() {
        LocalDateTime older = NOW.minusMinutes(2);

        Notification newer = save(NORMAL, NOW.minusMinutes(1));
        Notification first = save(NORMAL, older);
        Notification second = save(NORMAL, older);

        assertEquals(List.of(first.getId(), second.getId(), newer.getId()), candidates());
    }

    @Test
    void waitingRaisesEffectivePriorityAtFiveAndTenMinutes() {
        Notification lowUnderFive = save(LOW, NOW.minusMinutes(5).plusSeconds(1));
        Notification lowAtFive = save(LOW, NOW.minusMinutes(5));
        Notification lowAtTen = save(LOW, NOW.minusMinutes(10));
        Notification normal = save(NORMAL, NOW.minusMinutes(1));
        Notification high = save(HIGH, NOW);

        assertEquals(List.of(
                lowAtTen.getId(),
                high.getId(),
                lowAtFive.getId(),
                normal.getId(),
                lowUnderFive.getId()
        ), candidates());
    }

    @Test
    void futureRetryIsExcludedEvenWhenPriorityIsHigh() {
        Notification high = save(HIGH, NOW.minusMinutes(15));
        high.scheduleRetry(NOW.plusSeconds(1));

        Notification low = save(LOW, NOW);

        assertEquals(List.of(low.getId()), candidates());
    }

    private Notification save(NotificationPriority priority, LocalDateTime createdAt) {
        Notification notification =
                new Notification(List.of(), "Subject", "Body", CONSOLE, priority);

        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return repository.save(notification);
    }

    private List<Long> candidates() {
        repository.flush();

        return repository.findClaimCandidates(
                NotificationStatus.PENDING,
                NOW,
                HIGH,
                NORMAL,
                NOW.minusMinutes(5),
                NOW.minusMinutes(10),
                PageRequest.of(0, 10));
    }
}