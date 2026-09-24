package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.*;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.Spec;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.VulnerabilityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({KopierNaisApiData.class, TestTidsKonfigurasjon.class})
class KopierNaisApiDataTest {
    @MockitoBean
    HttpGraphQlClient graphql;
    @Autowired
    KopierNaisApiData kopierNaisApiData;
    @Autowired
    DeploymentRepository deploymentRepository;
    @Autowired
    Clock klokke;
    @MockitoBean
    ManifestParser manifestParser;
    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Test
    void testKopieringTilDatabase() {
        final GraphQlClient.RequestSpec mockRequestSpec = Mockito.mock(GraphQlClient.RequestSpec.class);
        final GraphQlClient.RetrieveSpec mockRetrieveSpec = Mockito.mock(GraphQlClient.RetrieveSpec.class);
        when(graphql.document(any())).thenReturn(mockRequestSpec);
        when(graphql.documentName(any())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.retrieve(any())).thenReturn(mockRetrieveSpec);
        when(manifestParser.parse(any())).thenReturn(new NaisManifest("TestJob", new Spec(List.of())));
        final Deployment deployment = new Deployment(LocalDateTime.now(klokke),
                "0123456789abcdef0123456789abcdef",
                "dependabot",
                "navikt/reposomikkeeksisterer");
        final Vulnerability suppressed = new Vulnerability("CVE-1", new Suppression(ImageVulnerabilitySuppressionState.NOT_AFFECTED));
        final Vulnerability ikkeSuppressed = new Vulnerability("CVE-2", null);
        final Image image = new Image("some.app", "v1.0", new PaginatedVulnerabilities(List.of(suppressed, ikkeSuppressed)));
        final List<Workload> workloads = List.of(new Workload("foo-things", Manifest.job("foo-things"), new PaginatedDeployments(List.of(deployment)), TeamEnvironment.of("test-fss"), image, new MiljøSpesifisertNavn("foo-things", "")));
        when(mockRetrieveSpec.toEntity(ArgumentMatchers.eq(Team.class))).thenReturn(Mono.just(new Team(new PaginatedWorkloads(workloads))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase();
        final List<no.nav.teamarbeidsforhold.githubapp.entity.Deployment> faktiskeDeploymentLagret = deploymentRepository.findAll();
        assertEquals(1, faktiskeDeploymentLagret.size());
        final no.nav.teamarbeidsforhold.githubapp.entity.Deployment deploymentLagret = faktiskeDeploymentLagret.getFirst();
        assertEquals("foo-things", deploymentLagret.getId().getWorkloadName());
        assertEquals("", deploymentLagret.getId().getSuffix());
        assertEquals("test-fss", deploymentLagret.getId().getEnvironment());
        assertEquals("TestJob", deploymentLagret.getWorkloadName().getWorkloadType());
        final List<no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability> sårbarheterLagret = vulnerabilityRepository.findAll();
        assertEquals(1, sårbarheterLagret.size());
        final no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability sårbarhetLagret = sårbarheterLagret.getFirst();
        assertEquals("CVE-2", sårbarhetLagret.getId());
    }

    @Test
    void testSkalParseScheduleFraManifest() throws Exception {
        final ManifestParser parser = new ManifestParser(new ObjectMapper(new YAMLFactory()));
        final NaisManifest manifest = parser.parse("""
                apiVersion: nais.io/v1alpha1
                kind: Application
                spec:
                  schedule:
                    - cron: "0 6 * * *"
                """);

        assertEquals("Application", manifest.kind());
        assertTrue(manifest.hasScheduledDeploy());
        assertEquals("0 6 * * *", manifest.spec().scheduleExpression());
    }

    @Test
    void testSkalParseScheduleFraOnBlokk() throws Exception {
        final ManifestParser parser = new ManifestParser(new ObjectMapper(new YAMLFactory()));
        final NaisManifest manifest = parser.parse("""
                apiVersion: nais.io/v1alpha1
                kind: Naisjob
                spec:
                  on:
                    schedule:
                      - cron: "0 7 * * *"
                """);

        assertEquals("Naisjob", manifest.kind());
        assertTrue(manifest.hasScheduledDeploy());
        assertEquals("0 7 * * *", manifest.spec().scheduleExpression());
    }

    @Test
    void testSkalParseScheduleFraGithubActionsOnBlokk() throws Exception {
        final ManifestParser parser = new ManifestParser(new ObjectMapper(new YAMLFactory()));
        final String yaml = """
                on:
                  workflow_dispatch:
                    inputs:
                      environment:
                        description: "Hvilket miljø skal appen deployes til?"
                        required: true
                        type: choice
                        options:
                          - dev
                          - q1
                          - q4
                          - q5
                          - all
                  schedule:
                    - cron: "0 1 * * 1"
                  push:
                    branches:
                      - main
                """;

        assertTrue(parser.hasScheduledDeploy(yaml));
    }

    @Test
    void testSkalGiFalseNarDetManglerSchedule() throws Exception {
        final ManifestParser parser = new ManifestParser(new ObjectMapper(new YAMLFactory()));
        final String yaml = """
                on:
                  workflow_dispatch:
                    inputs:
                      environment:
                        description: "Hvilket miljø skal appen deployes til?"
                        required: true
                        type: choice
                        options:
                          - dev
                          - q1
                          - q4
                          - q5
                          - all
                  push:
                    branches:
                      - main
                """;

        assertTrue(!parser.hasScheduledDeploy(yaml));
    }
}