package com.blood.notification.scheduler;

import com.blood.notification.channel.DispatchResult;
import com.blood.notification.channel.NotificationDispatcher;
import com.blood.notification.model.Notification;
import com.blood.notification.model.NotificationDeadLetter;
import com.blood.notification.model.NotificationStatus;
import com.blood.notification.repository.NotificationDeadLetterRepository;
import com.blood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Retry scheduler with exponential backoff:
 *   attempt 1 — immediately after PENDING is saved
 *   attempt 2 — 2 minutes after attempt 1
 *   attempt 3 — 4 minutes after attempt 2
 *   after attempt 3 fails → DEAD_LETTER (moved to notification_dead_letters)
 *
 * Resilience: because Spring Modulith stores unprocessed events in event_publication,
 * notifications are created even if the app was down at event time. This scheduler
 * then dispatches all accumulated PENDING notifications on restart — zero loss.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_SECONDS = {0L, 120L, 240L}; // 0, 2 min, 4 min

    private final NotificationRepository notificationRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationDispatcher dispatcher;

    @Scheduled(fixedDelay = 60_000) // run every minute
    @Transactional
    public void retryPendingNotifications() {
        List<Notification> candidates = notificationRepository.findDispatchEligible(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), MAX_RETRIES);

        if (candidates.isEmpty()) return;

        log.debug("Retry scheduler: {} candidate(s) to dispatch", candidates.size());

        for (Notification notification : candidates) {
            if (!isEligibleNow(notification)) continue;

            notification.setLastAttemptAt(Instant.now());
            notification.setRetryCount(notification.getRetryCount() + 1);

            DispatchResult result = dispatcher.dispatch(notification);

            if (result.anySucceeded()) {
                notification.setStatus(NotificationStatus.SENT);
                log.info("Notification dispatched: id={} attempt={}",
                        notification.getId(), notification.getRetryCount());
            } else {
                if (notification.getRetryCount() >= MAX_RETRIES) {
                    moveToDeadLetter(notification, result);
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                    log.warn("Notification delivery failed (attempt {}): id={} errors={}",
                            notification.getRetryCount(), notification.getId(), result.channelErrors());
                }
            }

            notificationRepository.save(notification);
        }
    }

    private boolean isEligibleNow(Notification notification) {
        Instant lastAttempt = notification.getLastAttemptAt();
        if (lastAttempt == null) return true; // never attempted

        int attempt = notification.getRetryCount(); // retries already done
        long backoff = attempt < BACKOFF_SECONDS.length ? BACKOFF_SECONDS[attempt] : BACKOFF_SECONDS[BACKOFF_SECONDS.length - 1];
        return Instant.now().isAfter(lastAttempt.plusSeconds(backoff));
    }

    private void moveToDeadLetter(Notification notification, DispatchResult result) {
        notification.setStatus(NotificationStatus.DEAD_LETTER);
        String reason = "All " + MAX_RETRIES + " dispatch attempts failed. Errors: " + result.channelErrors();

        deadLetterRepository.save(NotificationDeadLetter.builder()
                .notification(notification)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());

        log.error("Notification moved to DLQ: id={} reason={}", notification.getId(), reason);
    }
}
