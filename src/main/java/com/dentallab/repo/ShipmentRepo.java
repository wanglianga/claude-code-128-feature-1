package com.dentallab.repo;

import com.dentallab.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepo extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderIdOrderByIdDesc(Long orderId);
}
