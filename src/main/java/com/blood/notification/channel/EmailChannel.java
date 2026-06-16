package com.blood.notification.channel;

import com.blood.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub email channel. In production this would use JavaMailSender / SendGrid.
 * supports() returns false when the recipient carries no contact info at all,
 * so the notification is not silently swallowed — the gap is logged by the dispatcher.
 */
@Slf4j
@Component
public class EmailChannel implements NotificationChannel {

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean supports(Notification notification) {
        String recipient = notification.getRecipient();
        if (recipient == null || recipient.isBlank()) {
            log.debug("Email skipped: no recipient on notificationId={}", notification.getId());
            return false;
        }
        return true;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        log.info("[EMAIL] → recipient={} | message=\"{}\"",
                notification.getRecipient(), notification.getMessage());
    }
}
