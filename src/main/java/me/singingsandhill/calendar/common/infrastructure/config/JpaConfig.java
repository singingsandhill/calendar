package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
    "me.singingsandhill.calendar.datedate.infrastructure.persistence.repository",
    "me.singingsandhill.calendar.runner.infrastructure.persistence.repository",
    "me.singingsandhill.calendar.trading.infrastructure.persistence.repository"
})
public class JpaConfig {
}
