package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.components.ManifestParser;
import no.nav.teamarbeidsforhold.githubapp.config.JacksonKonfigurasjon;
import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.entity.DeploymentId;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressGroup;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressGroup.ExposureEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngressServiceTest {
    private final DeploymentRepository repository = mock(DeploymentRepository.class);
    private final IngressService service = new IngressService(repository,
            new ManifestParser(new JacksonKonfigurasjon().objectMapper()));

    @ParameterizedTest
    @CsvSource({
            "https://www.nav.no/arbeid, INTERNET",
            "https://app.ekstern.dev.nav.no, INTERNET",
            "https://app.ansatt.nav.no, EMPLOYEES",
            "https://app.ansatt.dev.nav.no, EMPLOYEES",
            "https://app.intern.nav.no, INTERNAL",
            "https://app.intern.dev.nav.no, INTERNAL",
            "https://app.nais.adeo.no, INTERNAL",
            "https://app.prod-fss-pub.nais.io, GCP",
            "https://app.dev-fss-pub.nais.io, GCP",
            "https://APP.INTERN.NAV.NO./path, INTERNAL",
            "https://intern.nav.no, INTERNAL",
            "https://app.example.org, UNKNOWN",
            "https://app.intern.nav.no.example.org, UNKNOWN",
            "https://notintern.nav.no, INTERNET",
            "https://intern.nav.no@app.example.org, UNKNOWN",
            "https://app.nav.no/path/intern.nav.no, INTERNET",
            "javascript:alert(1), UNKNOWN",
            "/relative/path, UNKNOWN",
            "ikke en URL, UNKNOWN"
    })
    void gruppererEtterHostIkkeTekstIUrl(final String url, final IngressGroup.ExposureEnum forventet) {
        when(repository.findAll()).thenReturn(List.of(deployment("app", "prod-gcp", "", "navikt/repo@sha",
                "kind: Application\nspec:\n  ingresses:\n    - '" + url + "'\n")));

        final var oversikt = service.oversikt();

        assertEquals(1, oversikt.getGroups().size());
        assertEquals(forventet, oversikt.getGroups().getFirst().getExposure());
        assertEquals(url, oversikt.getGroups().getFirst().getIngresses().getFirst().getUrl());
    }

    @Test
    void prioritererÅpneOgUavklarteFørInterneOgBeholderRepoOgMiljøPerIngress() {
        final String manifest = """
                kind: Application
                metadata:
                  name: app
                spec:
                  ingresses:
                    - https://app.intern.nav.no
                    - https://app.ansatt.nav.no
                    - https://app.prod-fss-pub.nais.io
                    - https://app.example.org
                    - https://www.nav.no/z
                    - https://www.nav.no/a
                    - https://www.nav.no/a
                """;
        final var prod = deployment("app", "prod-gcp", "", "https://github.com/navikt/repo.git@sha", manifest);
        final var dev = deployment("app-", "dev-gcp", "q1", "navikt/annet-repo@sha", manifest);
        final var sistSett = Instant.parse("2026-11-18T08:00:00Z");
        prod.setLastSeen(sistSett);
        when(repository.findAll()).thenReturn(List.of(dev, prod));

        final var oversikt = service.oversikt();

        assertEquals(List.of(INTERNET, UNKNOWN, EMPLOYEES, INTERNAL, GCP),
                oversikt.getGroups().stream().map(IngressGroup::getExposure).toList());
        final var offentlige = oversikt.getGroups().getFirst().getIngresses();
        assertEquals(4, offentlige.size());
        assertEquals(List.of("https://www.nav.no/a", "https://www.nav.no/a",
                "https://www.nav.no/z", "https://www.nav.no/z"), offentlige.stream().map(i -> i.getUrl()).toList());
        assertEquals("navikt/repo", offentlige.getFirst().getRepository());
        assertEquals("app", offentlige.getFirst().getWorkload());
        assertEquals("prod-gcp", offentlige.getFirst().getEnvironment());
        assertEquals(sistSett.atOffset(ZoneOffset.UTC), offentlige.getFirst().getLastSeen());
        assertEquals("navikt/annet-repo", offentlige.get(1).getRepository());
        assertEquals("q1", offentlige.get(1).getSuffix());
        assertEquals("dev-gcp", offentlige.get(1).getEnvironment());
        assertNull(offentlige.get(1).getLastSeen());
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void skillerManglendeManifesterFraApperUtenIngress() {
        when(repository.findAll()).thenReturn(List.of(
                deployment("mangler", "prod-gcp", "", null, null),
                deployment("tom", "prod-gcp", "", null, " "),
                deployment("jobb", "prod-gcp", "", null, "kind: Naisjob"),
                deployment("intern-app", "prod-gcp", "", null, "kind: Application\nspec:\n  image: app"),
                deployment("ingen", "prod-gcp", "", null, "kind: Application\nspec:\n  ingresses: []")));

        final var oversikt = service.oversikt();

        assertTrue(oversikt.getGroups().isEmpty());
        assertEquals(2, oversikt.getDeploymentsWithoutManifest());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"null@sha", "ukjent", "https://example.org/navikt/repo@sha",
            "navikt/repo/ekstra@sha", "https://github.com.evil.example/navikt/repo@sha"})
    void markererManglendeEllerUkjenteRepoer(final String lastCommit) {
        when(repository.findAll()).thenReturn(List.of(deployment("app", "prod-gcp", "", lastCommit,
                "spec:\n  ingresses:\n    - https://app.nav.no")));

        assertNull(service.oversikt().getGroups().getFirst().getIngresses().getFirst().getRepository());
    }

    @Test
    void feilerTydeligVedUleseligManifestFremforÅReturnereUfullstendigOversikt() {
        when(repository.findAll()).thenReturn(List.of(deployment("app", "prod-gcp", "", null,
                "spec: [ugyldig")));

        assertThrows(JacksonException.class, service::oversikt);
    }

    private static Deployment deployment(final String navn, final String miljø, final String suffix,
                                         final String lastCommit, final String manifest) {
        final var id = new DeploymentId();
        id.setWorkloadName(navn);
        id.setEnvironment(miljø);
        id.setSuffix(suffix);
        final var deployment = new Deployment();
        deployment.setId(id);
        deployment.setLastCommit(lastCommit);
        deployment.setManifest(manifest);
        return deployment;
    }
}
