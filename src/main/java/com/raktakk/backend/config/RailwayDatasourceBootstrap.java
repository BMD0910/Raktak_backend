package com.raktakk.backend.config;

import java.net.URI;

import org.springframework.util.StringUtils;

public final class RailwayDatasourceBootstrap {

    private RailwayDatasourceBootstrap() {
    }

    public static void apply() {
        String datasourceUrl = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_PUBLIC_URL"),
                System.getenv("DB_URL"));

        if (StringUtils.hasText(datasourceUrl)) {
            System.setProperty("spring.datasource.url", normalizeJdbcUrl(datasourceUrl));
        }

        String username = firstNonBlank(System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("DB_USERNAME"));
        if (!StringUtils.hasText(username) && StringUtils.hasText(datasourceUrl)) {
            username = extractUserInfo(datasourceUrl, 0);
        }
        if (StringUtils.hasText(username)) {
            System.setProperty("spring.datasource.username", username);
        }

        String password = firstNonBlank(System.getenv("SPRING_DATASOURCE_PASSWORD"), System.getenv("DB_PASSWORD"), System.getenv("PGPASSWORD"));
        if (!StringUtils.hasText(password) && StringUtils.hasText(datasourceUrl)) {
            password = extractUserInfo(datasourceUrl, 1);
        }
        if (StringUtils.hasText(password)) {
            System.setProperty("spring.datasource.password", password);
        }
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