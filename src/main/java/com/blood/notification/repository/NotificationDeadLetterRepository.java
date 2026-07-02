package com.blood.notification.repository;

import com.blood.notification.model.NotificationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, Long> {

    List<NotificationDeadLetter> findAllByOrderByCreatedAtDesc();
}
