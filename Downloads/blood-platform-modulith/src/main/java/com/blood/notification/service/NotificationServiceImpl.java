package com.blood.notification.service;

import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
