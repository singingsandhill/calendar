package me.singingsandhill.calendar.runner.application.service;

import me.singingsandhill.calendar.runner.domain.Admin;
import me.singingsandhill.calendar.runner.domain.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RunnerAdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public RunnerAdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Admin createAdmin(String username, String rawPassword) {
        if (adminRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Admin admin = new Admin(username, encodedPassword);
        return adminRepository.save(admin);
    }

    @Transactional
    public Admin getOrCreateAdmin(String username, String rawPassword) {
        return adminRepository.findByUsername(username)
                .orElseGet(() -> createAdmin(username, rawPassword));
    }

    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsername(username);
    }
}
