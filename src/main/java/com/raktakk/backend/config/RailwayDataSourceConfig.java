package com.raktakk.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("prod")
public class RailwayDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RailwayDataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment environment) {
        String jdbcUrl = resolveJdbcUrl(environment);
        String username = firstNonBlank(
                normalizeCandidate(get(environment, "SPRING_DATASOURCE_USERNAME")),
                normalizeCandidate(get(environment, "DB_USERNAME")),
                normalizeCandidate(get(environment, "PGUSER")),
                normalizeCandidate(get(environment, "POSTGRES_USER")));
        String password = firstNonBlank(
                normalizeCandidate(get(environment, "SPRING_DATASOURCE_PASSWORD")),
                normalizeCandidate(get(environment, "DB_PASSWORD")),
                normalizeCandidate(get(environment, "PGPASSWORD")),
                normalizeCandidate(get(environment, "POSTGRES_PASSWORD")));

        log.info("Railway datasource: mode=prod, urlSource={}, host={}, port={}, database={}, username={}, password={}",
                describe(get(environment, "SPRING_DATASOURCE_URL")),
                describe(get(environment, "PGHOST")),
                describe(get(environment, "PGPORT")),
                describe(firstNonBlank(get(environment, "PGDATABASE"), get(environment, "POSTGRES_DB"))),
                describe(username),
                describe(password));
        log.info("Railway datasource: finalJdbcUrl={}", maskJdbcUrl(jdbcUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }

    private String resolveJdbcUrl(Environment environment) {
        String direct = normalizeCandidate(get(environment, "SPRING_DATASOURCE_URL"));
        if (isUsable(direct)) {
            log.info("Railway datasource: mode=SPRING_DATASOURCE_URL");
            return toJdbcUrl(direct);
        }

        String databaseUrl = firstNonBlank(
                normalizeCandidate(get(environment, "DATABASE_URL")),
                normalizeCandidate(get(environment, "DATABASE_PUBLIC_URL")),
                normalizeCandidate(get(environment, "DB_URL")));
        if (isUsable(databaseUrl)) {
            log.info("Railway datasource: mode=DATABASE_URL");
            return toJdbcUrl(databaseUrl);
        }

        String host = normalizeCandidate(get(environment, "PGHOST"));
        String port = normalizeCandidate(get(environment, "PGPORT"));
        String database = firstNonBlank(normalizeCandidate(get(environment, "PGDATABASE")), normalizeCandidate(get(environment, "POSTGRES_DB")));
        if (isUsable(host) && isUsable(database)) {
            log.info("Railway datasource: mode=PGHOST_PGPORT_PGDATABASE");
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host.trim());
            if (isUsable(port)) {
                jdbc.append(':').append(port.trim());
            }
            jdbc.append('/').append(database.trim());
            jdbc.append("?sslmode=require");
            return jdbc.toString();
        }

        throw new IllegalStateException(
            "Aucune configuration datasource valide trouvée. Définissez SPRING_DATASOURCE_URL ou PGHOST/PGPORT/PGDATABASE dans Railway.");
    }

    private String toJdbcUrl(String value) {
        String candidate = value.trim();
        if (candidate.startsWith("jdbc:")) {
            return candidate;
        }

        if (candidate.startsWith("postgres://")) {
            return "jdbc:postgresql://" + candidate.substring("postgres://".length());
        }

        if (candidate.startsWith("postgresql://")) {
            return "jdbc:" + candidate;
        }

        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            if (scheme != null && scheme.toLowerCase(Locale.ROOT).startsWith("postgres")) {
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
            }
        } catch (URISyntaxException ignored) {
            // Fallback: return candidate as-is below.
        }

        return candidate;
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
}