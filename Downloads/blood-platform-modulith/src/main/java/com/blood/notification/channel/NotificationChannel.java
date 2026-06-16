package com.blood.notification.channel;

import com.blood.notification.model.Notification;

/**
 * OCP: each delivery channel (SMS, email, …) implements this interface.
 * The dispatcher depends on this abstraction — adding a new channel never
 * requires modifying existing code.
 */
public interface NotificationChannel {

    ChannelType type();

    /**
     * Returns true if this channel can attempt delivery for the given notification.
     * A channel that requires a phone number should return false when the recipient
     * carries no phone information — partial-channel failure does not fail the whole
     * notification dispatch.
     */
    boolean supports(Notification notification);

    /**
     * Attempt delivery. Throws {@link NotificationChannelException} on failure so
     * the dispatcher can record the error and decide whether to retry.
     */
    void send(Notification notification) throws NotificationChannelException;
}
