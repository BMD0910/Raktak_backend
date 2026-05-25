package com.raktakk.backend.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Permet de démarrer le backend aussi bien en local qu'en production Railway.
 *
 * Railway expose généralement {@code DATABASE_URL}, {@code PGUSER},
 * {@code PGPASSWORD} et parfois {@code DATABASE_PUBLIC_URL}. Spring attend
 * par défaut un JDBC URL de la forme {@code jdbc:postgresql://...}; ce
 * post-processor convertit donc automatiquement les URLs Railway vers ce format
 * et aligne les variables de datasource Spring si elles ne sont pas déjà définies.
 */
public class RailwayDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "railwayDatasourceOverrides";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> overrides = new LinkedHashMap<>();

		String springUrl = normalizeCandidate(environment.getProperty("SPRING_DATASOURCE_URL"));
		String databaseUrl = normalizeCandidate(firstNonBlank(
				environment.getProperty("DATABASE_URL"),
				environment.getProperty("DATABASE_PUBLIC_URL"),
				environment.getProperty("DB_URL")));
		String pgUrl = buildJdbcUrlFromParts(
				normalizeCandidate(environment.getProperty("PGHOST")),
				normalizeCandidate(environment.getProperty("PGPORT")),
				normalizeCandidate(firstNonBlank(environment.getProperty("PGDATABASE"), environment.getProperty("POSTGRES_DB"))));

		String resolvedUrl = normalizeJdbcUrl(firstNonBlank(springUrl, databaseUrl, pgUrl));
		if (StringUtils.hasText(resolvedUrl)) {
			overrides.put("SPRING_DATASOURCE_URL", resolvedUrl);
			overrides.put("spring.datasource.url", resolvedUrl);
			overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
		}

		String username = firstNonBlank(
				normalizeCandidate(environment.getProperty("SPRING_DATASOURCE_USERNAME")),
				normalizeCandidate(environment.getProperty("DB_USERNAME")),
				normalizeCandidate(environment.getProperty("PGUSER")),
				normalizeCandidate(environment.getProperty("POSTGRES_USER")));
		if (StringUtils.hasText(username)) {
			overrides.put("SPRING_DATASOURCE_USERNAME", username);
			overrides.put("spring.datasource.username", username);
		}

		String password = firstNonBlank(
				normalizeCandidate(environment.getProperty("SPRING_DATASOURCE_PASSWORD")),
				normalizeCandidate(environment.getProperty("DB_PASSWORD")),
				normalizeCandidate(environment.getProperty("PGPASSWORD")),
				normalizeCandidate(environment.getProperty("POSTGRES_PASSWORD")));
		if (StringUtils.hasText(password)) {
			overrides.put("SPRING_DATASOURCE_PASSWORD", password);
			overrides.put("spring.datasource.password", password);
		}

		if (!overrides.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private String normalizeJdbcUrl(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		String candidate = value.trim();
		if (candidate.startsWith("jdbc:")) {
			return candidate;
		}

		if (candidate.startsWith("postgres://") || candidate.startsWith("postgresql://")) {
			return "jdbc:" + candidate;
		}

		try {
			URI uri = URI.create(candidate);
			String scheme = uri.getScheme();
			if ("postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme)) {
				StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
				if (StringUtils.hasText(uri.getHost())) {
					jdbc.append(uri.getHost());
				}
				if (uri.getPort() > 0) {
					jdbc.append(':').append(uri.getPort());
				}
				if (StringUtils.hasText(uri.getPath())) {
					jdbc.append(uri.getPath());
				}
				if (StringUtils.hasText(uri.getQuery())) {
					jdbc.append('?').append(uri.getQuery());
				}
				return jdbc.toString();
			}
		} catch (IllegalArgumentException ignored) {
			// Si la valeur n'est pas une URI valide, on la renvoie telle quelle.
		}

		return candidate;
	}

	private String buildJdbcUrlFromParts(String host, String port, String database) {
		if (!StringUtils.hasText(host) || !StringUtils.hasText(database)) {
			return null;
		}

		StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host.trim());
		if (StringUtils.hasText(port)) {
			jdbc.append(':').append(port.trim());
		}
		jdbc.append('/').append(database.trim());
		jdbc.append("?sslmode=require");
		return jdbc.toString();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String normalizeCandidate(String value) {
		String candidate = trimToNull(value);
		if (!StringUtils.hasText(candidate)) {
			return null;
		}
		if (candidate.contains("${")) {
			return null;
		}
		return candidate;
	}
}
