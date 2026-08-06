package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.entity.*;
import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.ImageVulnerabilitySuppressionState;
import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.MiljøSpesifisertNavn;
import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.Team;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.VulnerabilityRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.WorkloadRepository;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class KopierNaisApiData {
    private final HttpGraphQlClient naisApi;
    private final DeploymentRepository deploymentRepository;
    private final ManifestParser manifestParser;
    private final Clock klokke;
    private final WorkloadRepository workloadRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository;

    public KopierNaisApiData(final HttpGraphQlClient naisApi, final DeploymentRepository deploymentRepository, final ManifestParser manifestParser, final Clock klokke, final WorkloadRepository workloadRepository, final VulnerabilityRepository vulnerabilityRepository, final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository) {
        this.naisApi = naisApi;
        this.deploymentRepository = deploymentRepository;
        this.manifestParser = manifestParser;
        this.klokke = klokke;
        this.workloadRepository = workloadRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.deploymentVulnerabilityRepository = deploymentVulnerabilityRepository;
    }

    @Async
    public void kopierNaisApiDataTilDatabase() {
        naisApi.document("workloads-med-critical-cve").retrieve("team").toEntity(Team.class).subscribe(this::lagre, throwable -> log.error("Feil i henting eller lagring av data fra nais console", throwable));
    }

    private void lagre(final Team team) {
        final List<Workload> nyeWorkloads = new ArrayList<>();
        final List<Deployment> nyeDeployments = new ArrayList<>();
        final List<DeploymentVulnerability> nyeSårbarheterFunnet = new ArrayList<>();
        final List<Vulnerability> nyeSårbarheter = new ArrayList<>();
        team.workloads().stream().flatMap(workload -> workload.image().vulnerabilities().stream()
                        .filter(sårbarhet -> sårbarhet.suppression() == null)
                        .map(sårbarhet -> sårbarhet.identifier()))
                .distinct()
                .forEach(sårbarhet -> {
                    Vulnerability vulnerability = new Vulnerability();
                    vulnerability.setId(sårbarhet);
                    nyeSårbarheter.add(vulnerability);
                });
        team.workloads().forEach(workload -> {
            final Workload nyWorkload = new Workload();
            final String manifestInnhold = workload.manifest().content();
            final NaisManifest manifest = manifestParser.parse(manifestInnhold);
            nyWorkload.setWorkloadType(manifest.kind());
            final MiljøSpesifisertNavn miljøSpesifisertNavn = workload.miljøSpesifisertNavn();
            nyWorkload.setName(miljøSpesifisertNavn.navn());
            nyWorkload.setImage(workload.image().navnMedTag());
            nyeWorkloads.add(nyWorkload);
            final String cluster = workload.teamEnvironment().environment().name();
            workload.deployments().forEach(deployment -> {
                final Deployment nyDeployment = new Deployment();
                final DeploymentId id = new DeploymentId();
                id.setWorkloadName(miljøSpesifisertNavn.navn());
                id.setSuffix(miljøSpesifisertNavn.miljø());
                id.setEnvironment(cluster);
                nyDeployment.setId(id);
                nyDeployment.setLastSeen(Instant.now(klokke));
                nyDeployment.setManifest(manifestInnhold);
                nyDeployment.setLastDeployer(deployment.deployerUsername());
                nyDeployment.setWorkloadName(nyWorkload);
                nyDeployment.setWorkloadType(manifest.kind());
                nyDeployment.setLastCommit(deployment.repository() + "@" + deployment.commitSha());
                nyDeployment.setLastDeployTime(deployment.createdAt().atZone(klokke.getZone()).toInstant());
                nyeDeployments.add(nyDeployment);
                workload.image().vulnerabilities().stream().filter(sårbarhet -> sårbarhet.suppression() == null).forEach(sårbarhet -> {
                    final DeploymentVulnerability sårbarhetViHar = new DeploymentVulnerability();
                    final DeploymentVulnerabilityId sårbarhetsId = new DeploymentVulnerabilityId();
                    sårbarhetsId.setVulnerabilityId(sårbarhet.identifier());
                    sårbarhetsId.setWorkloadName(id.getWorkloadName());
                    sårbarhetsId.setEnvironment(id.getEnvironment());
                    sårbarhetsId.setSuffix(id.getSuffix());
                    sårbarhetViHar.setId(sårbarhetsId);
                    sårbarhetViHar.setDeployment(nyDeployment);
                    nyeSårbarheterFunnet.add(sårbarhetViHar);
                });
            });
        });
        vulnerabilityRepository.saveAll(nyeSårbarheter);
        workloadRepository.saveAll(nyeWorkloads);
        deploymentRepository.saveAll(nyeDeployments);
        deploymentVulnerabilityRepository.saveAll(nyeSårbarheterFunnet);
    }
}
