package com.blood.notification.listener;

import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.notification.model.Notification;
import com.blood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonorRegisteredListener {

    private final NotificationRepository notificationRepository;

    @ApplicationModuleListener
    public void onDonorRegistered(DonorRegisteredEvent event) {
        String message = "Welcome " + event.fullName() + "! Your donor registration is confirmed. Blood type: " + event.bloodType();
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification saved for donor: {}", event.fullName());
    }
}
