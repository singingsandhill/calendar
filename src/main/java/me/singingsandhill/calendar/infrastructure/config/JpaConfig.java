package me.singingsandhill.calendar.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "me.singingsandhill.calendar.infrastructure.persistence.repository")
public class JpaConfig {
}
