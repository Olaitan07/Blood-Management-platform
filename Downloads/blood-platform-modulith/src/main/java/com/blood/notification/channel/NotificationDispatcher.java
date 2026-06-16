package com.blood.notification.channel;

import com.blood.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OCP: new channels are picked up automatically via the injected list —
 * this class never needs to change when a channel is added or removed.
 *
 * Partial-failure policy: if at least one channel succeeds the overall result
 * is SUCCEEDED.  A failed channel is logged but does not block remaining channels.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final List<NotificationChannel> channels;

    public DispatchResult dispatch(Notification notification) {
        boolean anySucceeded = false;
        List<String> errors = new ArrayList<>();
        boolean anySupportedChannel = false;

        for (NotificationChannel channel : channels) {
            if (!channel.supports(notification)) {
                continue;
            }
            anySupportedChannel = true;
            try {
                channel.send(notification);
                anySucceeded = true;
                log.debug("Channel {} succeeded for notificationId={}", channel.type(), notification.getId());
            } catch (NotificationChannelException ex) {
                log.warn("Channel {} failed for notificationId={}: {}",
                        channel.type(), notification.getId(), ex.getMessage());
                errors.add(ex.getMessage());
            }
        }

        if (!anySupportedChannel) {
            // No channel could reach this recipient — log the gap, treat as success so
            // it doesn't retry forever; the in-app record is the delivery.
            log.warn("No channel supported notificationId={} recipient={} — in-app only",
                    notification.getId(), notification.getRecipient());
            return DispatchResult.allSucceeded();
        }

        return anySucceeded
                ? new DispatchResult(true, errors)
                : DispatchResult.allFailed(errors);
    }
}
