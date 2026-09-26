package com.dentallab.repo;

import com.dentallab.model.ToothItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToothItemRepo extends JpaRepository<ToothItem, Long> {
    List<ToothItem> findByOrderIdOrderByIdAsc(Long orderId);

    List<ToothItem> findByShipmentId(Long shipmentId);
}
