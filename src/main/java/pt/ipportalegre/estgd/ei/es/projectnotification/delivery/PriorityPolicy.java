package pt.ipportalegre.estgd.ei.es.projectnotification.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class PriorityPolicy {

    private final Duration normalAfter;
    private final Duration highAfter;

    public PriorityPolicy(
            @Value("${app.delivery.priority.normal-after:5m}") String normalAfter,
            @Value("${app.delivery.priority.high-after:10m}") String highAfter) {
        this.normalAfter = DurationStyle.detectAndParse(normalAfter);
        this.highAfter = DurationStyle.detectAndParse(highAfter);

        if (this.normalAfter.isZero() || this.normalAfter.isNegative()
                || this.highAfter.compareTo(this.normalAfter) <= 0) {
            throw new IllegalArgumentException(
                    "Priority thresholds must satisfy 0 < normal-after < high-after");
        }
    }

    public LocalDateTime normalCutoff(LocalDateTime now) {
        return now.minus(normalAfter);
    }

    public LocalDateTime highCutoff(LocalDateTime now) {
        return now.minus(highAfter);
    }
}