package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.dto.Team;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class KopierNaisApiData {
    private final HttpGraphQlClient naisApi;
    private final DeploymentRepository deploymentRepository;

    public KopierNaisApiData(final HttpGraphQlClient naisApi, final DeploymentRepository deploymentRepository) {
        this.naisApi = naisApi;
        this.deploymentRepository = deploymentRepository;
    }

    @Async
    public void kopierNaisApiDataTilDatabase() {
        final Mono<Team> team = naisApi.document("workloads-med-critical-cve").retrieve("team").toEntity(Team.class);
        log.info("Team ble {}", team.block());
    }
}
