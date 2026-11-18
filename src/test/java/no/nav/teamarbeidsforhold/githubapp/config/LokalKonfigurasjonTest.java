package no.nav.teamarbeidsforhold.githubapp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LokalKonfigurasjonTest {
    private MockEnvironment lokaltMiljo() {
        final MockEnvironment miljo = new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/statistikk")
                .withProperty("spring.liquibase.url", "jdbc:postgresql://127.0.0.1:5432/statistikk");
        miljo.setActiveProfiles("local");
        return miljo;
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "localhost", "[::1]"})
    void tillaterKunLokaleDatabaseadresser(final String host) {
        final MockEnvironment miljo = lokaltMiljo()
                .withProperty("spring.datasource.url", "jdbc:postgresql://" + host + ":5433/eksempler")
                .withProperty("spring.liquibase.url", "jdbc:postgresql://" + host + ":5433/eksempler");
        assertDoesNotThrow(() -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
    }

    @Test
    void avviserKombinerteOgImplisitteProfiler() {
        final MockEnvironment miljo = lokaltMiljo();
        miljo.setActiveProfiles("local", "prod");
        assertThrows(IllegalStateException.class, () -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
        miljo.setActiveProfiles();
        miljo.setDefaultProfiles("local");
        assertThrows(IllegalStateException.class, () -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.profiles.include", "spring.profiles.include[0]"})
    void avviserInkludertLokalprofil(final String navn) {
        final MockEnvironment miljo = lokaltMiljo().withProperty(navn, "local");
        assertThrows(IllegalStateException.class, () -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
    }

    @Test
    void stopperOppstartFoerDatabaseOgSikkerhetskjedeOpprettes() {
        new ApplicationContextRunner()
                .withUserConfiguration(LokalKonfigurasjon.class)
                .withInitializer(kontekst -> kontekst.getEnvironment().setActiveProfiles("local", "prod"))
                .run(kontekst -> {
                    final Throwable feil = kontekst.getStartupFailure();
                    assertNotNull(feil);
                    assertTrue(feil.getMessage().contains("explicitly and alone"));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"NAIS_CLUSTER_NAME", "NAIS_APP_NAME", "KUBERNETES_SERVICE_HOST",
            "READWRITE_PGJDBCURL", "PGJDBCURL", "NAIS_DATABASE_APP_URL"})
    void avviserDriftsmiljoOgArvedeDatabaser(final String navn) {
        final MockEnvironment miljo = lokaltMiljo().withProperty(navn, "ikke-lokalt");
        assertThrows(IllegalStateException.class, () -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://database.example.test/statistikk",
            "jdbc:postgresql://127.0.0.1.example.test/statistikk",
            "jdbc:postgresql://127.0.0.1,remote.example.test/statistikk",
            "jdbc:postgresql://127.0.0.1/statistikk?PGHOST=remote.example.test",
            "jdbc:postgresql://local:password@127.0.0.1/statistikk",
            "jdbc:postgresql://127.0.0.1/statistikk#fragment",
            "jdbc:postgresql://127.0.0.1/",
            "jdbc:postgresql://[invalid/statistikk",
            "jdbc:h2:mem:test"
    })
    void avviserEksterneOgTvetydigeDatabaseadresser(final String url) {
        for (final String navn : new String[]{"spring.datasource.url", "spring.liquibase.url",
                "spring.datasource.hikari.jdbc-url"}) {
            final MockEnvironment miljo = lokaltMiljo().withProperty(navn, url);
            assertThrows(IllegalStateException.class, () -> LokalKonfigurasjon.validerLokaltMiljo(miljo));
        }
    }
}
