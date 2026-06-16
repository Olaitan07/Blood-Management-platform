package com.blood.notification.channel;

import com.blood.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stub SMS channel.  In production this would delegate to Twilio or a similar provider.
 * Set {@code notification.sms.force-fail=true} to simulate provider outage and exercise
 * the retry / dead-letter path without a real SMS gateway.
 *
 * supports() returns false when the recipient carries no phone info (starts with "email:")
 * so that SMS is skipped and only email is attempted — demonstrating partial-channel failure.
 */
@Slf4j
@Component
public class SmsChannel implements NotificationChannel {

    @Value("${notification.sms.force-fail:false}")
    private boolean forceFail;

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public boolean supports(Notification notification) {
        String recipient = notification.getRecipient();
        if (recipient == null || recipient.isBlank()) {
            log.debug("SMS skipped: no recipient on notificationId={}", notification.getId());
            return false;
        }
        // email-only recipients are not reachable via SMS
        if (recipient.startsWith("email:")) {
            log.debug("SMS skipped: email-only recipient on notificationId={}", notification.getId());
            return false;
        }
        return true;
    }

    @Override
    public void send(Notification notification) throws NotificationChannelException {
        if (forceFail) {
            throw new NotificationChannelException(ChannelType.SMS,
                    "Simulated provider failure (notification.sms.force-fail=true)");
        }
        log.info("[SMS] → recipient={} | message=\"{}\"",
                notification.getRecipient(), notification.getMessage());
    }
}
