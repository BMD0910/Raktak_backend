package com.raktakk.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class RailwayDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String jdbcUrl = resolveJdbcUrl();
        String username = firstNonBlank(
                cleanEnv("SPRING_DATASOURCE_USERNAME"),
                cleanEnv("DB_USERNAME"),
                cleanEnv("PGUSER"),
                cleanEnv("POSTGRES_USER"));
        String password = firstNonBlank(
                cleanEnv("SPRING_DATASOURCE_PASSWORD"),
                cleanEnv("DB_PASSWORD"),
                cleanEnv("PGPASSWORD"),
                cleanEnv("POSTGRES_PASSWORD"));

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

    private String resolveJdbcUrl() {
        String direct = cleanEnv("SPRING_DATASOURCE_URL");
        if (isUsable(direct)) {
            return toJdbcUrl(direct);
        }

        String databaseUrl = firstNonBlank(cleanEnv("DATABASE_URL"), cleanEnv("DATABASE_PUBLIC_URL"), cleanEnv("DB_URL"));
        if (isUsable(databaseUrl)) {
            return toJdbcUrl(databaseUrl);
        }

        String host = cleanEnv("PGHOST");
        String port = cleanEnv("PGPORT");
        String database = firstNonBlank(cleanEnv("PGDATABASE"), cleanEnv("POSTGRES_DB"));
        if (isUsable(host) && isUsable(database)) {
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

        if (candidate.startsWith("postgres://") || candidate.startsWith("postgresql://")) {
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

    private String cleanEnv(String key) {
        String value = System.getenv(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.contains("${")) {
            return null;
        }
        return trimmed;
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
}