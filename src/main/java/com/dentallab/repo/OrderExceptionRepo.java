package com.dentallab.repo;

import com.dentallab.model.OrderException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderExceptionRepo extends JpaRepository<OrderException, Long> {
    List<OrderException> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderException> findByStatusOrderByIdDesc(com.dentallab.model.ExceptionStatus status);
}
