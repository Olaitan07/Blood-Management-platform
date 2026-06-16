package com.blood.notification.channel;

/**
 * Thrown by a {@link NotificationChannel} when delivery fails.
 * The message describes the failure reason for dead-letter logging.
 */
public class NotificationChannelException extends RuntimeException {

    private final ChannelType channelType;

    public NotificationChannelException(ChannelType channelType, String reason) {
        super("[" + channelType + "] " + reason);
        this.channelType = channelType;
    }

    public NotificationChannelException(ChannelType channelType, String reason, Throwable cause) {
        super("[" + channelType + "] " + reason, cause);
        this.channelType = channelType;
    }

    public ChannelType getChannelType() {
        return channelType;
    }
}
