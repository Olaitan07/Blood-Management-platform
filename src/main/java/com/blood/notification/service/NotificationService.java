package com.blood.notification.service;

import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> listNotifications();

    List<NotificationResponse> listByHospital(Long hospitalId, Authentication auth);

    List<NotificationResponse> listByDonor(Long donorId, Authentication auth);

    List<DeadLetterResponse> listDeadLetters();

    NotificationResponse markRead(Long notificationId, Authentication auth);

    void markAllReadForHospital(Long hospitalId, Authentication auth);

    void markAllReadForDonor(Long donorId, Authentication auth);
}
