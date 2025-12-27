package me.singingsandhill.calendar.domain.runner;

import java.util.Optional;

public interface AdminRepository {

    Optional<Admin> findByUsername(String username);

    Admin save(Admin admin);

    boolean existsByUsername(String username);
}
