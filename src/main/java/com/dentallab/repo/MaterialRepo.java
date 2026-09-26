package com.dentallab.repo;

import com.dentallab.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialRepo extends JpaRepository<Material, Long> {
    Optional<Material> findByCode(String code);
}
