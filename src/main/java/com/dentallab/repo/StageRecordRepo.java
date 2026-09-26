package com.dentallab.repo;

import com.dentallab.model.StageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageRecordRepo extends JpaRepository<StageRecord, Long> {
    List<StageRecord> findByToothItemIdOrderByIdAsc(Long toothItemId);

    List<StageRecord> findByOperatorId(Long operatorId);
}
