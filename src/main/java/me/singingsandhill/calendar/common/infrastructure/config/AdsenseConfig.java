package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdsenseProperties.class)
public class AdsenseConfig {
}
