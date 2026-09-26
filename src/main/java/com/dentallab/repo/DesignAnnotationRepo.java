package com.dentallab.repo;

import com.dentallab.model.DesignAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignAnnotationRepo extends JpaRepository<DesignAnnotation, Long> {
    List<DesignAnnotation> findByToothItemIdOrderByIdAsc(Long toothItemId);
}
