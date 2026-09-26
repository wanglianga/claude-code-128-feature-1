package com.dentallab.repo;

import com.dentallab.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepo extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByOrderId(Long orderId);

    List<Settlement> findByOrderIdIn(Collection<Long> orderIds);
}
