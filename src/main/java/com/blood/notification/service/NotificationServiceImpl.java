package com.blood.notification.service;

import com.blood.notification.dto.DeadLetterResponse;
import com.blood.notification.dto.NotificationResponse;
import com.blood.notification.repository.NotificationDeadLetterRepository;
import com.blood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByHospital(Long hospitalId) {
        return notificationRepository.findByHospitalIdOrderBySentAtDesc(hospitalId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByDonor(Long donorId) {
        return notificationRepository.findByDonorIdOrderBySentAtDesc(donorId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeadLetterResponse> listDeadLetters() {
        return deadLetterRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DeadLetterResponse::from)
                .toList();
    }
}
