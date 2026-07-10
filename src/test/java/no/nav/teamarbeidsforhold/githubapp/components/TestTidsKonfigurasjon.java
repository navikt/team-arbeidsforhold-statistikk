package no.nav.teamarbeidsforhold.githubapp.components;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestTidsKonfigurasjon {
    @Bean
    public Clock clock() {
        return Clock.fixed(
                Instant.parse("2126-01-01T00:00:00Z"),
                ZoneId.of("Europe/Oslo")
        );
    }
}
