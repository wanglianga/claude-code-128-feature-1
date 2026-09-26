package com.dentallab.repo;

import com.dentallab.model.ScanFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanFileRepo extends JpaRepository<ScanFile, Long> {
    List<ScanFile> findByOrderIdOrderByIdAsc(Long orderId);
}
