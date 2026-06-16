package com.blood.notification.channel;

import java.util.List;

/**
 * Value object returned by {@link NotificationDispatcher} after attempting all channels.
 * anySucceeded=true  → overall status SENT (partial failure is tolerated).
 * anySucceeded=false → overall status FAILED (all channels failed; schedule retry).
 */
public record DispatchResult(boolean anySucceeded, List<String> channelErrors) {

    public static DispatchResult allSucceeded() {
        return new DispatchResult(true, List.of());
    }

    public static DispatchResult allFailed(List<String> errors) {
        return new DispatchResult(false, errors);
    }

    public boolean hasErrors() {
        return !channelErrors.isEmpty();
    }
}
