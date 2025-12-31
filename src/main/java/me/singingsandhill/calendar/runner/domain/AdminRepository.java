package me.singingsandhill.calendar.runner.domain;

import java.util.Optional;

public interface AdminRepository {

    Optional<Admin> findByUsername(String username);

    Admin save(Admin admin);

    boolean existsByUsername(String username);
}
