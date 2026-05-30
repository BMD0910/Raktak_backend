package com.raktakk.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment environment) {
        DataSourceSettings settings = resolveDataSourceSettings(environment);

        log.info("Prod datasource: mode={}, urlSource={}, host={}, port={}, database={}, username={}, password={}",
                settings.mode,
                describe(get(environment, "SPRING_DATASOURCE_URL")),
                describe(get(environment, "PGHOST")),
                describe(get(environment, "PGPORT")),
                describe(firstNonBlank(get(environment, "PGDATABASE"), get(environment, "POSTGRES_DB"))),
                describe(settings.username),
                describe(settings.password));
        log.info("Prod datasource: finalJdbcUrl={}", maskJdbcUrl(settings.jdbcUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl);
        if (settings.username != null) {
            config.setUsername(settings.username);
        }
        if (settings.password != null) {
            config.setPassword(settings.password);
        }
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }

    private DataSourceSettings resolveDataSourceSettings(Environment environment) {
        String direct = normalizeCandidate(get(environment, "SPRING_DATASOURCE_URL"));
        if (isUsable(direct)) {
            log.info("Prod datasource: mode=SPRING_DATASOURCE_URL");
            return new DataSourceSettings(
                    toJdbcUrl(direct),
                    firstNonBlank(normalizeCandidate(get(environment, "SPRING_DATASOURCE_USERNAME")), normalizeCandidate(get(environment, "PGUSER")), normalizeCandidate(get(environment, "POSTGRES_USER"))),
                    firstNonBlank(normalizeCandidate(get(environment, "SPRING_DATASOURCE_PASSWORD")), normalizeCandidate(get(environment, "PGPASSWORD")), normalizeCandidate(get(environment, "POSTGRES_PASSWORD"))),
                    "SPRING_DATASOURCE_URL");
        }

        String databaseUrl = firstNonBlank(
                normalizeCandidate(get(environment, "DATABASE_URL")),
                normalizeCandidate(get(environment, "DATABASE_PUBLIC_URL")),
                normalizeCandidate(get(environment, "DB_URL")));
        if (isUsable(databaseUrl)) {
            log.info("Prod datasource: mode=DATABASE_URL");
            return fromUri(databaseUrl, environment, "DATABASE_URL");
        }

        String host = normalizeCandidate(get(environment, "PGHOST"));
        String port = normalizeCandidate(get(environment, "PGPORT"));
        String database = firstNonBlank(normalizeCandidate(get(environment, "PGDATABASE")), normalizeCandidate(get(environment, "POSTGRES_DB")));
        if (isUsable(host) && isUsable(database)) {
            log.info("Prod datasource: mode=PGHOST_PGPORT_PGDATABASE");
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host.trim());
            if (isUsable(port)) {
                jdbc.append(':').append(port.trim());
            }
            jdbc.append('/').append(database.trim());
            return new DataSourceSettings(
                    jdbc.toString(),
                    firstNonBlank(normalizeCandidate(get(environment, "PGUSER")), normalizeCandidate(get(environment, "POSTGRES_USER"))),
                    firstNonBlank(normalizeCandidate(get(environment, "PGPASSWORD")), normalizeCandidate(get(environment, "POSTGRES_PASSWORD"))),
                    "PGHOST_PGPORT_PGDATABASE");
        }

        throw new IllegalStateException(
                "Aucune configuration datasource valide trouvée. Définissez SPRING_DATASOURCE_URL/SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD ou les variables PostgreSQL Render.");
    }

    private DataSourceSettings fromUri(String candidate, Environment environment, String mode) {
        try {
            URI uri = new URI(candidate.trim());
            String jdbcUrl = toJdbcUrl(candidate);
            String username = firstNonBlank(
                    normalizeCandidate(get(environment, "SPRING_DATASOURCE_USERNAME")),
                    normalizeCandidate(get(environment, "PGUSER")),
                    normalizeCandidate(get(environment, "POSTGRES_USER")),
                    userFromUri(uri));
            String password = firstNonBlank(
                    normalizeCandidate(get(environment, "SPRING_DATASOURCE_PASSWORD")),
                    normalizeCandidate(get(environment, "PGPASSWORD")),
                    normalizeCandidate(get(environment, "POSTGRES_PASSWORD")),
                    passwordFromUri(uri));
            return new DataSourceSettings(jdbcUrl, username, password, mode);
        } catch (URISyntaxException ex) {
            return new DataSourceSettings(
                    toJdbcUrl(candidate),
                    firstNonBlank(normalizeCandidate(get(environment, "SPRING_DATASOURCE_USERNAME")), normalizeCandidate(get(environment, "PGUSER")), normalizeCandidate(get(environment, "POSTGRES_USER"))),
                    firstNonBlank(normalizeCandidate(get(environment, "SPRING_DATASOURCE_PASSWORD")), normalizeCandidate(get(environment, "PGPASSWORD")), normalizeCandidate(get(environment, "POSTGRES_PASSWORD"))),
                    mode);
        }
    }

    private String toJdbcUrl(String value) {
        String candidate = value.trim();
        if (candidate.startsWith("jdbc:")) {
            return candidate;
        }

        if (candidate.startsWith("postgres://") || candidate.startsWith("postgresql://")) {
            try {
                URI uri = new URI(candidate);
                StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
                if (uri.getHost() != null) {
                    jdbc.append(uri.getHost());
                }
                if (uri.getPort() > 0) {
                    jdbc.append(':').append(uri.getPort());
                }
                if (uri.getPath() != null) {
                    jdbc.append(uri.getPath());
                }
                if (uri.getQuery() != null) {
                    jdbc.append('?').append(uri.getQuery());
                }
                return jdbc.toString();
            } catch (URISyntaxException ex) {
                if (candidate.startsWith("postgres://")) {
                    return "jdbc:postgresql://" + candidate.substring("postgres://".length());
                }
                return "jdbc:" + candidate;
            }
        }

        return candidate;
    }

    private String userFromUri(URI uri) {
        if (uri.getUserInfo() == null || uri.getUserInfo().isBlank()) {
            return null;
        }
        String[] parts = uri.getUserInfo().split(":", 2);
        return parts.length > 0 ? normalizeCandidate(parts[0]) : null;
    }

    private String passwordFromUri(URI uri) {
        if (uri.getUserInfo() == null || uri.getUserInfo().isBlank()) {
            return null;
        }
        String[] parts = uri.getUserInfo().split(":", 2);
        return parts.length == 2 ? normalizeCandidate(parts[1]) : null;
    }

    private String get(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }

    private boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (isUsable(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeCandidate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.contains("${")) {
            return null;
        }
        return trimmed;
    }

    private String describe(String value) {
        if (value == null) {
            return "absent";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "blank";
        }
        if (trimmed.contains("${")) {
            return "placeholder";
        }
        return "present";
    }

    private String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "null";
        }
        String masked = jdbcUrl;
        int passwordIndex = masked.toLowerCase(Locale.ROOT).indexOf("password=");
        if (passwordIndex >= 0) {
            int end = masked.indexOf('&', passwordIndex);
            if (end < 0) {
                end = masked.length();
            }
            masked = masked.substring(0, passwordIndex + "password=".length()) + "***" + masked.substring(end);
        }
        int userInfoIndex = masked.indexOf("//");
        int atIndex = masked.indexOf('@', userInfoIndex + 2);
        if (userInfoIndex >= 0 && atIndex > userInfoIndex) {
            masked = masked.substring(0, userInfoIndex + 2) + "***:***@" + masked.substring(atIndex + 1);
        }
        return masked;
    }

    private record DataSourceSettings(String jdbcUrl, String username, String password, String mode) {}
}