package com.dentallab.repo;

import com.dentallab.model.TryInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TryInRecordRepo extends JpaRepository<TryInRecord, Long> {
    List<TryInRecord> findByOrderIdOrderByIdAsc(Long orderId);
}
