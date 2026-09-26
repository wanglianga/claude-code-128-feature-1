package com.dentallab.repo;

import com.dentallab.model.DentureOrder;
import com.dentallab.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DentureOrderRepo extends JpaRepository<DentureOrder, Long> {
    Optional<DentureOrder> findByOrderNo(String orderNo);

    List<DentureOrder> findByClinicIdOrderByIdDesc(Long clinicId);

    List<DentureOrder> findByStatusOrderByIdDesc(OrderStatus status);

    List<DentureOrder> findByClinicIdAndStatusOrderByIdDesc(Long clinicId, OrderStatus status);

    List<DentureOrder> findAllByOrderByIdDesc();

    List<DentureOrder> findByClinicIdAndCompletedAtBetween(Long clinicId, LocalDateTime from, LocalDateTime to);
}
