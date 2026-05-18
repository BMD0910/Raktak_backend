package com.raktakk.backend.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

public class RailwayDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "railwayDatasourceOverrides";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new HashMap<>();

        String datasourceUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_PUBLIC_URL"),
                environment.getProperty("DB_URL"));

        if (StringUtils.hasText(datasourceUrl)) {
            overrides.put("spring.datasource.url", normalizeJdbcUrl(datasourceUrl));
        }

        String username = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("DB_USERNAME"));
        if (!StringUtils.hasText(username) && StringUtils.hasText(datasourceUrl)) {
            username = extractUserInfo(datasourceUrl, 0);
        }
        if (StringUtils.hasText(username)) {
            overrides.put("spring.datasource.username", username);
        }

        String password = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("DB_PASSWORD"),
                environment.getProperty("PGPASSWORD"));
        if (!StringUtils.hasText(password) && StringUtils.hasText(datasourceUrl)) {
            password = extractUserInfo(datasourceUrl, 1);
        }
        if (StringUtils.hasText(password)) {
            overrides.put("spring.datasource.password", password);
        }

        if (!overrides.isEmpty()) {
            PropertySource<?> propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, overrides);
            environment.getPropertySources().addFirst(propertySource);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeJdbcUrl(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        return url;
    }

    private static String extractUserInfo(String url, int index) {
        try {
            URI uri = URI.create(url);
            String userInfo = uri.getUserInfo();
            if (!StringUtils.hasText(userInfo)) {
                return null;
            }
            String[] parts = userInfo.split(":", 2);
            if (index == 0) {
                return parts.length > 0 ? parts[0] : null;
            }
            return parts.length > 1 ? parts[1] : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}