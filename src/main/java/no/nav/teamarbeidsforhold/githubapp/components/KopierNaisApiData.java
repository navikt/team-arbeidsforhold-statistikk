package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.entity.*;
import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.MiljøSpesifisertNavn;
import no.nav.teamarbeidsforhold.githubapp.naisapi.dto.Team;
import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.VulnerabilityRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.WorkloadRepository;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!local")
public class KopierNaisApiData {
    private static final Duration MAKSIMAL_VENTETID_FOR_API_RESULTAT = Duration.ofMinutes(5);

    private final HttpGraphQlClient naisApi;
    private final DeploymentRepository deploymentRepository;
    private final ManifestParser manifestParser;
    private final Clock klokke;
    private final WorkloadRepository workloadRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository;
    private final TransactionTemplate transactionTemplate;

    public KopierNaisApiData(final HttpGraphQlClient naisApi, final DeploymentRepository deploymentRepository, final ManifestParser manifestParser, final Clock klokke, final WorkloadRepository workloadRepository, final VulnerabilityRepository vulnerabilityRepository, final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository, final TransactionTemplate transactionTemplate) {
        this.naisApi = naisApi;
        this.deploymentRepository = deploymentRepository;
        this.manifestParser = manifestParser;
        this.klokke = klokke;
        this.workloadRepository = workloadRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.deploymentVulnerabilityRepository = deploymentVulnerabilityRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public CompletableFuture<Void> kopierNaisApiDataTilDatabase() {
        return naisApi.documentName("workloads-med-critical-cve").retrieve("team").toEntity(Team.class)
                .timeout(MAKSIMAL_VENTETID_FOR_API_RESULTAT)
                .doOnNext(team -> transactionTemplate.executeWithoutResult(_ -> lagre(team)))
                .then().toFuture();
    }

    private void lagre(final Team team) {
        final List<Workload> nyeWorkloads = new ArrayList<>();
        final List<Deployment> nyeDeployments = new ArrayList<>();
        final List<DeploymentVulnerability> nyeSårbarheterFunnet = new ArrayList<>();
        final List<Vulnerability> nyeSårbarheter = new ArrayList<>();
        team.workloads().nodes().stream().flatMap(workload -> workload.image().vulnerabilities().nodes().stream()
                        .filter(sårbarhet -> sårbarhet.suppression() == null)
                        .map(sårbarhet -> sårbarhet.identifier()))
                .distinct()
                .forEach(sårbarhet -> {
                    Vulnerability vulnerability = new Vulnerability();
                    vulnerability.setId(sårbarhet);
                    nyeSårbarheter.add(vulnerability);
                });
        team.workloads().nodes().forEach(workload -> {
            final Workload nyWorkload = new Workload();
            final String manifestInnhold = workload.manifest().content();
            final NaisManifest manifest = manifestParser.parse(manifestInnhold);
            nyWorkload.setWorkloadType(manifest.kind());
            final MiljøSpesifisertNavn miljøSpesifisertNavn = workload.miljøSpesifisertNavn();
            nyWorkload.setName(miljøSpesifisertNavn.navn());
            nyWorkload.setImage(workload.image().navnMedTag());
            nyeWorkloads.add(nyWorkload);
            final String cluster = workload.teamEnvironment().environment().name();
            workload.deployments().nodes().forEach(deployment -> {
                final Deployment nyDeployment = new Deployment();
                final DeploymentId id = new DeploymentId();
                id.setWorkloadName(miljøSpesifisertNavn.navn());
                id.setSuffix(miljøSpesifisertNavn.miljø() == null ? "" : miljøSpesifisertNavn.miljø());
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
                workload.image().vulnerabilities().nodes().stream().filter(sårbarhet -> sårbarhet.suppression() == null).forEach(sårbarhet -> {
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
        final Set<DeploymentVulnerabilityId> gjeldendeSårbarhetsIder = nyeSårbarheterFunnet.stream()
                .map(DeploymentVulnerability::getId).collect(Collectors.toSet());
        nyeDeployments.stream().map(Deployment::getId).distinct().forEach(id -> deploymentVulnerabilityRepository.deleteAll(
                deploymentVulnerabilityRepository.findAllByDeployment_Id(id).stream()
                        .filter(sårbarhet -> !gjeldendeSårbarhetsIder.contains(sårbarhet.getId()))
                        .toList()));
        nyeSårbarheterFunnet.forEach(sårbarhet ->
                sårbarhet.setVulnerability(vulnerabilityRepository.getReferenceById(sårbarhet.getId().getVulnerabilityId())));
        deploymentVulnerabilityRepository.saveAll(nyeSårbarheterFunnet);
    }
}
