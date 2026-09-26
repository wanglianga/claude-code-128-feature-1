package com.dentallab.repo;

import com.dentallab.model.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepo extends JpaRepository<Clinic, Long> {
}
