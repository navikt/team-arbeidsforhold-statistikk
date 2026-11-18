package no.nav.teamarbeidsforhold.githubapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebSecurity
@Profile("local")
public class LokalKonfigurasjon {
    @Bean
    static BeanFactoryPostProcessor kontrollerLokaltMiljo(final ConfigurableEnvironment miljo) {
        return _ -> {
            validerLokaltMiljo(miljo);
            log.info("Local profile active: anonymous loopback access, external imports and GitHub updates disabled");
        };
    }

    static void validerLokaltMiljo(final ConfigurableEnvironment miljo) {
        if (!Arrays.equals(miljo.getActiveProfiles(), new String[]{"local"})) {
            throw new IllegalStateException("The local profile must be activated explicitly and alone");
        }
        if (!Set.of("127.0.0.1", "localhost", "::1", "[::1]")
                .contains(miljo.getProperty("server.address", ""))) {
            throw new IllegalStateException("The local profile must bind HTTP to a loopback address");
        }
        if (Binder.get(miljo).bind("spring.profiles.include", Bindable.listOf(String.class))
                .orElse(List.of()).contains("local")) {
            throw new IllegalStateException("The local profile cannot be activated through spring.profiles.include");
        }
        for (final String navn : Set.of("NAIS_CLUSTER_NAME", "NAIS_APP_NAME", "KUBERNETES_SERVICE_HOST",
                "READWRITE_PGJDBCURL", "PGJDBCURL")) {
            if (miljo.containsProperty(navn)) {
                throw new IllegalStateException("The local profile cannot be used with " + navn);
            }
        }
        for (final PropertySource<?> kilde : miljo.getPropertySources()) {
            if (kilde instanceof EnumerablePropertySource<?> egenskaper
                    && Arrays.stream(egenskaper.getPropertyNames()).anyMatch(navn -> navn.startsWith("NAIS_DATABASE_"))) {
                throw new IllegalStateException("The local profile cannot be used with NAIS_DATABASE_* settings");
            }
        }
        for (final String navn : Set.of("spring.datasource.url", "spring.liquibase.url")) {
            validerDatabaseUrl(navn, miljo.getProperty(navn));
        }
        final String hikariUrl = miljo.getProperty("spring.datasource.hikari.jdbc-url");
        if (hikariUrl != null) {
            validerDatabaseUrl("spring.datasource.hikari.jdbc-url", hikariUrl);
        }
    }

    private static void validerDatabaseUrl(final String navn, final String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(navn + " must be a loopback PostgreSQL URL");
        }
        final URI uri;
        try {
            uri = new URI(url.substring("jdbc:".length()));
        } catch (URISyntaxException _) {
            throw new IllegalStateException(navn + " must be a valid loopback PostgreSQL URL");
        }
        if (!Set.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost() == null ? "" : uri.getHost())
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getPath() == null || !uri.getPath().matches("/[A-Za-z0-9_-]+")) {
            throw new IllegalStateException(navn + " must use a loopback host and a database name, without URL parameters");
        }
    }

    @Bean
    SecurityFilterChain lokalSecurityFilterChain(final HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").denyAll()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
