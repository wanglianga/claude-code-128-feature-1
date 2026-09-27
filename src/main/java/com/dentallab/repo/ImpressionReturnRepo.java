package com.dentallab.repo;

import com.dentallab.model.ImpressionReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpressionReturnRepo extends JpaRepository<ImpressionReturn, Long> {
    List<ImpressionReturn> findByOrderIdOrderByIdAsc(Long orderId);
}
