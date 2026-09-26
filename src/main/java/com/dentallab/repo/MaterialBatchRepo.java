package com.dentallab.repo;

import com.dentallab.model.MaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialBatchRepo extends JpaRepository<MaterialBatch, Long> {
    List<MaterialBatch> findByMaterialIdOrderByIdAsc(Long materialId);
}
