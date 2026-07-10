package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.*;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void kopierNaisApiDataTilDatabase() {
        final GraphQlClient.RequestSpec mockRequestSpec = Mockito.mock(GraphQlClient.RequestSpec.class);
        final GraphQlClient.RetrieveSpec mockRetrieveSpec = Mockito.mock(GraphQlClient.RetrieveSpec.class);
        when(graphql.document(any())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.retrieve(any())).thenReturn(mockRetrieveSpec);
        final Deployment deployment = new Deployment(LocalDateTime.now(klokke),
                "0123456789abcdef0123456789abcdef",
                "dependabot",
                "navikt/reposomikkeeksisterer");
        final Vulnerability suppressed = new Vulnerability("CVE-1", new Suppression(ImageVulnerabilitySuppressionState.NOT_AFFECTED));
        final Vulnerability ikkeSuppressed = new Vulnerability("CVE-2", null);
        final Image image = new Image("some.app", "v1.0", List.of(suppressed, ikkeSuppressed));
        final List<Workload> workloads = List.of(new Workload("foo-things", Manifest.job("foo-things"), List.of(deployment), TeamEnvironment.of("test-fss"), image));
        when(mockRetrieveSpec.toEntity(ArgumentMatchers.eq(Team.class))).thenReturn(Mono.just(new Team(workloads)));
        kopierNaisApiData.kopierNaisApiDataTilDatabase();
        assertEquals(List.of(new no.nav.teamarbeidsforhold.githubapp.entity.Deployment()), deploymentRepository.findAll());
    }
}