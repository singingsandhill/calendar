package me.singingsandhill.calendar.runner.infrastructure.config;

import me.singingsandhill.calendar.runner.application.service.RunnerAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RunnerAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RunnerAdminInitializer.class);

    private final RunnerAdminService adminService;

    @Value("${runner.admin.username:admin}")
    private String adminUsername;

    @Value("${runner.admin.password:admin123}")
    private String adminPassword;

    public RunnerAdminInitializer(RunnerAdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void run(String... args) {
        if (!adminService.existsByUsername(adminUsername)) {
            adminService.createAdmin(adminUsername, adminPassword);
            log.info("Runner admin account created: {}", adminUsername);
        } else {
            log.info("Runner admin account already exists: {}", adminUsername);
        }
    }
}
