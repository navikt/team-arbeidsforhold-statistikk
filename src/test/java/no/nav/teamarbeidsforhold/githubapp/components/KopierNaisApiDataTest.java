package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.*;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.Spec;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Autowired
    private DeploymentVulnerabilityRepository deploymentVulnerabilityRepository;

    @Test
    void test_kopiering_til_database() {
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
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();
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
    void fjerner_rettede_sårbarheter_bare_fra_oppdatert_deployment() {
        final Vulnerability rettet = new Vulnerability("CVE-RETTET", null);
        final Vulnerability beholdt = new Vulnerability("CVE-BEHOLDT", null);
        final Vulnerability ny = new Vulnerability("CVE-NY", null);
        final GraphQlClient.RetrieveSpec svar = mockNaisSvar();
        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(rettet, beholdt)),
                workload("app", "", "prod-fss", List.of(rettet)),
                workload("app", "q1", "prod-gcp", List.of(rettet)),
                workload("annen-app", "", "prod-gcp", List.of(rettet)))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(beholdt, ny)))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        assertThat(deploymentVulnerabilityRepository.findAll())
                .extracting(sårbarhet -> List.of(sårbarhet.getId().getWorkloadName(),
                        sårbarhet.getId().getSuffix(), sårbarhet.getId().getEnvironment(),
                        sårbarhet.getId().getVulnerabilityId()))
                .containsExactlyInAnyOrder(
                        List.of("app", "", "prod-gcp", "CVE-BEHOLDT"),
                        List.of("app", "", "prod-gcp", "CVE-NY"),
                        List.of("app", "", "prod-fss", "CVE-RETTET"),
                        List.of("app", "q1", "prod-gcp", "CVE-RETTET"),
                        List.of("annen-app", "", "prod-gcp", "CVE-RETTET"));
    }

    @Test
    void fjerner_alle_sårbarheter_når_nais_returnerer_tom_liste() {
        final GraphQlClient.RetrieveSpec svar = mockNaisSvar();
        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(new Vulnerability("CVE-RETTET", null))))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of()))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        assertThat(deploymentVulnerabilityRepository.findAll()).isEmpty();
        assertThat(deploymentRepository.findAll()).hasSize(1);
        assertThat(vulnerabilityRepository.findAll())
                .extracting(no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability::getId)
                .containsExactly("CVE-RETTET");
    }

    @Test
    void fjerner_sårbarheter_som_har_blitt_undertrykt() {
        final GraphQlClient.RetrieveSpec svar = mockNaisSvar();
        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(new Vulnerability("CVE-UNDERTRYKT", null))))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        final Vulnerability undertrykt = new Vulnerability("CVE-UNDERTRYKT",
                new Suppression(ImageVulnerabilitySuppressionState.NOT_AFFECTED));
        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(undertrykt)))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        assertThat(deploymentVulnerabilityRepository.findAll()).isEmpty();
    }

    @Test
    void beholder_sårbarheter_når_henting_fra_nais_feiler() {
        final GraphQlClient.RetrieveSpec svar = mockNaisSvar();
        when(svar.toEntity(Team.class)).thenReturn(Mono.just(new Team(new PaginatedWorkloads(List.of(
                workload("app", "", "prod-gcp", List.of(new Vulnerability("CVE-BEHOLDT", null))))))));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().join();

        when(svar.toEntity(Team.class)).thenReturn(Mono.error(new IllegalStateException("Henting feilet")));

        assertThrows(CompletionException.class, () -> kopierNaisApiData.kopierNaisApiDataTilDatabase().join());
        assertThat(deploymentVulnerabilityRepository.findAll())
                .extracting(sårbarhet -> sårbarhet.getId().getVulnerabilityId())
                .containsExactly("CVE-BEHOLDT");
    }

    private GraphQlClient.RetrieveSpec mockNaisSvar() {
        final GraphQlClient.RequestSpec forespørsel = Mockito.mock(GraphQlClient.RequestSpec.class);
        final GraphQlClient.RetrieveSpec svar = Mockito.mock(GraphQlClient.RetrieveSpec.class);
        when(graphql.documentName(any())).thenReturn(forespørsel);
        when(forespørsel.retrieve(any())).thenReturn(svar);
        when(manifestParser.parse(any())).thenReturn(new NaisManifest("Application", new Spec(List.of())));
        return svar;
    }

    private Workload workload(final String navn, final String suffix, final String miljø,
                              final List<Vulnerability> sårbarheter) {
        final Deployment deployment = new Deployment(LocalDateTime.now(klokke),
                "0123456789abcdef0123456789abcdef", "dependabot", "navikt/reposomikkeeksisterer");
        return new Workload(navn, Manifest.job(navn), new PaginatedDeployments(List.of(deployment)),
                TeamEnvironment.of(miljø), new Image("some.app", "v1.0", new PaginatedVulnerabilities(sårbarheter)),
                new MiljøSpesifisertNavn(navn, suffix));
    }
}