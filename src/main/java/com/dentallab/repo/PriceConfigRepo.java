package com.dentallab.repo;

import com.dentallab.model.PriceConfig;
import com.dentallab.model.ProsthesisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceConfigRepo extends JpaRepository<PriceConfig, Long> {
    Optional<PriceConfig> findByProsthesisType(ProsthesisType type);
}
