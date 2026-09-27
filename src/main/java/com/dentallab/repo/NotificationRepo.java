package com.dentallab.repo;

import com.dentallab.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByOrderIdOrderByIdAsc(Long orderId);
}
