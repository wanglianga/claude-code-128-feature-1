package com.dentallab.repo;

import com.dentallab.model.ImpressionReturn;
import com.dentallab.model.ImpressionReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ImpressionReturnRepo extends JpaRepository<ImpressionReturn, Long> {
    List<ImpressionReturn> findByOrderIdOrderByIdAsc(Long orderId);

    List<ImpressionReturn> findByOrderIdAndStatus(Long orderId, ImpressionReturnStatus status);

    List<ImpressionReturn> findByStatusInOrderByIdDesc(Collection<ImpressionReturnStatus> statuses);

    List<ImpressionReturn> findByOrderIdIn(Collection<Long> orderIds);
}
