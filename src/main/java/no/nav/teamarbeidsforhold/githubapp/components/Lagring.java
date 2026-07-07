package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.entity.DeploymentId;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Transactional
public class Lagring {
    private final RepoRepository repoRepository;

    public Lagring(final RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    public List<Repo> alleRepoer() {
        final String repoer = """
                aareg
                aareg-ajourholdsfiler
                aareg-aktor-sync
                aareg-analyse
                aareg-aura
                aareg-batch
                aareg-dist-api
                aareg-dist-api-contract
                aareg-dist-mottak
                aareg-dist-mottak-contract
                aareg-dist-online
                aareg-dist-online-api
                aareg-dist-sync
                aareg-dist-sync-endringer
                aareg-dolly-api
                aareg-gamle-arbeidsgivernavn
                aareg-innsyn-saksbehandler
                aareg-melding-pusher
                aareg-meldingsbehandling
                aareg-meldingsdefinisjon
                aareg-metrics-pusher
                aareg-otp-api
                aareg-services
                aareg-services-contract
                aareg-status
                aareg-teknisk-historikk
                aareg-tenor-adapter
                aareg-test-container
                aareg-tilgangskontroll
                aareg-tjenestespesifikasjoner
                aareg-uttrekk
                aareg-uttrekk-eksternt
                aareg-vedlikehold
                abac-aareg-core
                abac-brreg-proxy
                abac-registre-aareg
                arbeid-og-inntekt
                arbeidsgiver-innsyn-aareg
                arbeidsgiver-innsyn-aareg-api
                brreg-proxy
                dataprodukt-register-aareg
                dataprodukt-register-ereg
                ereg-aura
                ereg-services
                ereg-solr-enhetssok
                nav-api-portal
                nav-maskinporten
                team-arbeidsforhold-github-runner
                team-arbeidsforhold-statistikk
                team-arbeidsforhold-utvikling
                vault-iac
                """;
        return Arrays.stream(repoer.split("\\s+")).map(navn -> new Repo(navn, URI.create("https://github.com/" + navn), 0, 0)).toList();
        //return repoRepository.listAll().stream().map(repoEntitet -> new Repo(repoEntitet.getFullName(), repoEntitet.getCloneUrl(), 0)).toList();
    }

    public List<Deployment> alleDeployments() {
        final String naisTabell = """
                aareg-ajourholdsfiler                         | dev-fss     |             | COMPLETED   | 0               | 0          \s
                aareg-behandling                              | prod-fss    |             | RUNNING     | 20              | 1          \s
                aareg-behandling-orkestrator                  | prod-fss    |             | RUNNING     | 25              | 1          \s
                aareg-behandling-orkestrator                  | dev-fss     | q1          | RUNNING     | 26              | 1          \s
                aareg-behandling-orkestrator                  | dev-fss     | q2          | RUNNING     | 24              | 1          \s
                aareg-behandling-orkestrator                  | dev-fss     | q4          | RUNNING     | 26              | 1          \s
                aareg-behandling-orkestrator                  | dev-fss     | q5          | RUNNING     | 25              | 1          \s
                aareg-behandling                              | dev-fss     | q1          | RUNNING     | 24              | 1          \s
                aareg-behandling                              | dev-fss     | q4          | RUNNING     | 20              | 1          \s
                aareg-behandling                              | dev-fss     | q5          | RUNNING     | 23              | 1          \s
                aareg-dist-api                                | dev-fss     |             | RUNNING     | 181             | 1          \s
                aareg-dist-api                                | prod-fss    |             | RUNNING     | 181             | 2          \s
                aareg-dist-api                                | prod-gcp    |             | RUNNING     | 15              | 1          \s
                aareg-dist-api                                | dev-gcp     | q2          | RUNNING     | 30              | 1          \s
                aareg-dist-mottak                             | dev-fss     |             | RUNNING     | 29              | 1          \s
                aareg-dist-mottak                             | prod-fss    |             | RUNNING     | 29              | 1          \s
                aareg-dist-mottak                             | dev-fss     | q1          | RUNNING     | 32              | 1          \s
                aareg-dist-online                             | prod-gcp    |             | RUNNING     | 33              | 1          \s
                aareg-dist-online-api                         | dev-fss     |             | RUNNING     | 32              | 1          \s
                aareg-dist-online-api                         | prod-fss    |             | RUNNING     | 32              | 1          \s
                aareg-dist-online-api                         | dev-fss     | q1          | RUNNING     | 28              | 1          \s
                aareg-dist-online-api                         | dev-fss     | q2          | RUNNING     | 28              | 1          \s
                aareg-dist-online-gcp                         | dev-gcp     |             | RUNNING     | 33              | 1          \s
                aareg-dist-online                             | dev-gcp     | q1          | RUNNING     | 29              | 1          \s
                aareg-dist-online   -frontend                 | dev-gcp     |             | RUNNING     | 30              | 1          \s
                aareg-dist-online                             | dev-gcp     | q2          | RUNNING     | 29              | 1          \s
                aareg-dist-online   -frontend                 | dev-gcp     |             | RUNNING     | 30              | 1          \s
                aareg-dist-sync                               | dev-fss     |             | RUNNING     | 32              | 1          \s
                aareg-dist-sync                               | prod-fss    |             | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer                     | dev-fss     |             | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer                     | prod-fss    |             | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer                     | dev-fss     | q1          | RUNNING     | 32              | 1          \s
                aareg-dist-sync                               | dev-fss     |             | RUNNING     | 32              | 1          \s
                aareg-dolly-api                               | dev-fss     |             | RUNNING     | 18              | 1          \s
                aareg-dolly-api                               | dev-fss     |             | RUNNING     | 18              | 1          \s
                aareg-dolly-api                               | dev-fss     |             | RUNNING     | 18              | 1          \s
                aareg-dolly-api                               | dev-fss     |             | RUNNING     | 18              | 1          \s
                aareg-dolly-api                               | dev-fss     |             | RUNNING     | 18              | 1          \s
                aareg-hendelser-test                          | dev-fss     | q1          | RUNNING     | 18              | 1          \s
                aareg-hendelser-test                          | dev-fss     | q5          | RUNNING     | 18              | 1          \s
                aareg-innsyn-arbeidsgiver                     | dev-gcp     |             | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver                     | prod-gcp    |             | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver-api                 | dev-fss     |             | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver-api                 | prod-fss    |             | RUNNING     | 29              | 1          \s
                aareg-maskinporten-token                      | dev-fss     |             | RUNNING     | 16              | 1          \s
                aareg-mottak-arbeidsforhold                   | prod-fss    |             | RUNNING     | 23              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser         | prod-fss    |             | RUNNING     | 23              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser         | dev-fss     | q1          | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser         | dev-fss     | q5          | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold                   | dev-fss     | q1          | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold                   | dev-fss     | q4          | RUNNING     | 29              | 1          \s
                aareg-mottak-arbeidsforhold                   | dev-fss     | q5          | RUNNING     | 24              | 1          \s
                aareg-mottak-hendelser-vakt                   | prod-fss    |             | COMPLETED   | 27              | 1          \s
                aareg-mottak-opptjeningsgrunnlag              | prod-fss    |             | RUNNING     | 23              | 1          \s
                aareg-mottak-opptjeningsgrunnlag-hendelser    | prod-fss    |             | RUNNING     | 23              | 1          \s
                aareg-mottak-opptjeningsgrunnlag-hendelser    | dev-fss     | q4          | RUNNING     | 29              | 1          \s
                aareg-mottak-opptjeningsgrunnlag              | dev-fss     | q4          | RUNNING     | 29              | 1          \s
                aareg-oppdater-jurjur-hendelser               | dev-fss     | q4          | FAILED      | 25              | 2          \s
                aareg-oppdater-jurjur-hendelser               | dev-fss     | q5          | FAILED      | 25              | 2          \s
                aareg-otp-api                                 | dev-fss     |             | RUNNING     | 177             | 2          \s
                aareg-otp-api                                 | dev-gcp     |             | RUNNING     | 15              | 1          \s
                aareg-otp-api                                 | prod-fss    |             | RUNNING     | 177             | 2          \s
                aareg-otp-api                                 | prod-gcp    |             | RUNNING     | 15              | 1          \s
                aareg-otp-api                                 | dev-gcp     | q2          | RUNNING     | 15              | 1          \s
                aareg-patch-funk-endret                       | prod-fss    |             | FAILED      | 27              | 2          \s
                aareg-services-nais                           | dev-fss     |             | RUNNING     | 30              | 2          \s
                aareg-services-nais                           | prod-fss    |             | RUNNING     | 30              | 1          \s
                aareg-services-nais                           | dev-fss     | q0          | RUNNING     | 30              | 2          \s
                aareg-services-nais                           | dev-fss     | q1          | RUNNING     | 27              | 2          \s
                aareg-services-nais                           | dev-fss     | q4          | RUNNING     | 15              | 2          \s
                aareg-services-nais                           | dev-fss     | q5          | RUNNING     | 27              | 2          \s
                aareg-status                                  | dev-gcp     |             | RUNNING     | 1               | 0          \s
                aareg-status                                  | prod-gcp    |             | RUNNING     | 1               | 0          \s
                aareg-tenor-adapter                           | dev-gcp     |             | RUNNING     | 35              | 1          \s
                aareg-tilgangskontroll                        | prod-gcp    |             | RUNNING     | 17              | 1          \s
                aareg-tilgangskontroll                        | dev-gcp     | q1          | RUNNING     | 17              | 1          \s
                aareg-tilgangskontroll                        | dev-gcp     | q2          | RUNNING     | 17              | 1          \s
                aareg-to-bq                                   | prod-fss    |             | COMPLETED   | 1               | 0          \s
                aareg-uttrekk-dhuk                            | prod-fss    |             | UNKNOWN     | 18              | 1          \s
                baareg004                                     | dev-fss     |             | COMPLETED   | 4               | 0          \s
                baareg004                                     | prod-fss    |             | COMPLETED   | 4               | 0          \s
                baareg010                                     | dev-fss     |             | RUNNING     | 3               | 0          \s
                baareg010                                     | prod-fss    |             | COMPLETED   | 35              | 1          \s
                baareg013-konkurser                           | prod-fss    |             | COMPLETED   | 3               | 0          \s
                demo-aareg-innsyn-arbeidsgiver                | dev-gcp     |             | RUNNING     | 29              | 1          \s
                ereg-aura-runner                              | dev-fss     |             | NOT_RUNNING | 0               | 2          \s
                ereg-avansert-sok                             | dev-fss     |             | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | prod-fss    |             | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | dev-fss     | q0          | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | dev-fss     | q1          | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | dev-fss     | q4          | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | dev-fss     | q5          | UNKNOWN     | 4               | 0          \s
                ereg-services                                 | dev-fss     |             | RUNNING     | 1               | 1          \s
                ereg-services                                 | prod-fss    |             | RUNNING     | 1               | 1          \s
                ereg-services                                 | dev-fss     | q0          | RUNNING     | 1               | 1          \s
                ereg-services                                 | dev-fss     | q1          | RUNNING     | 1               | 1          \s
                ereg-services                                 | dev-fss     | q4          | RUNNING     | 1               | 1          \s
                ereg-services                                 | dev-fss     | q5          | RUNNING     | 1               | 1          \s
                team-arbeidsforhold-statistikk                | dev-gcp     |             | RUNNING     | 1               | 0     \s
                """;
        return Arrays.stream(naisTabell.split("\n")).map(linje -> workload(linje.split(Pattern.quote("|")))).toList();
    }

    private Deployment workload(final String[] felt) {
        final DeploymentId id = new DeploymentId();
        id.setWorkloadName(felt[0].trim());
        id.setEnvironment(felt[1].trim());
        id.setSuffix(felt[2].trim());
        final Deployment deployment = new Deployment();
        deployment.setId(id);
        deployment.setLastSeen(Instant.now());
        return deployment;
    }

    public RepoDetails repoMedNavn(final String repoName) {
        final List<Deployment> mineDeployments = alleDeployments().stream().filter(d -> d.getId().getWorkloadName().equals(repoName) || d.getId().getWorkloadName().startsWith(repoName) && d.getId().getWorkloadName().substring(repoName.length()).matches("-\\w\\d")).toList();
        return new RepoDetails(repoName, URI.create("https://github.com/" + repoName), 0, mineDeployments.size() - 2, mineDeployments.stream().map(Deployment::getId).map(DeploymentId::getWorkloadName).distinct().toList(), mineDeployments.stream().map(Deployment::getId).map(DeploymentId::getEnvironment).distinct().toList(), !mineDeployments.isEmpty());
    }
}
