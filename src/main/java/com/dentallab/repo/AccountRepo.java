package com.dentallab.repo;

import com.dentallab.model.Account;
import com.dentallab.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);

    List<Account> findByRole(Role role);
}
