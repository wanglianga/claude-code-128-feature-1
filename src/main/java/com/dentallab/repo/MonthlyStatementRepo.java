package com.dentallab.repo;

import com.dentallab.model.MonthlyStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyStatementRepo extends JpaRepository<MonthlyStatement, Long> {
    Optional<MonthlyStatement> findByClinicIdAndYearMonth(Long clinicId, String yearMonth);

    List<MonthlyStatement> findByClinicIdOrderByYearMonthDesc(Long clinicId);
}
