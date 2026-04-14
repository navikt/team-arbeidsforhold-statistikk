package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.entity.DeploymentId;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class DatabaseService {
    private final RepoRepository repoRepository;

    public DatabaseService(final RepoRepository repoRepository) {
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
        return Arrays.stream(repoer.split("\\s+")).map(navn -> new Repo(navn, "https://github.com/" + navn, 0)).toList();
        //return repoRepository.listAll().stream().map(repoEntitet -> new Repo(repoEntitet.getFullName(), repoEntitet.getCloneUrl(), 0)).toList();
    }

    public List<Deployment> alleDeployments() {
        final String naisTabell = """
                aareg-ajourholdsfiler                         | dev-fss     | Job         | COMPLETED   | 0               | 0          \s
                aareg-behandling                              | prod-fss    | Application | RUNNING     | 20              | 1          \s
                aareg-behandling-orkestrator                  | prod-fss    | Application | RUNNING     | 25              | 1          \s
                aareg-behandling-orkestrator-q1               | dev-fss     | Application | RUNNING     | 26              | 1          \s
                aareg-behandling-orkestrator-q2               | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-behandling-orkestrator-q4               | dev-fss     | Application | RUNNING     | 26              | 1          \s
                aareg-behandling-orkestrator-q5               | dev-fss     | Application | RUNNING     | 25              | 1          \s
                aareg-behandling-q1                           | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-behandling-q4                           | dev-fss     | Application | RUNNING     | 20              | 1          \s
                aareg-behandling-q5                           | dev-fss     | Application | RUNNING     | 23              | 1          \s
                aareg-dist-api                                | dev-fss     | Application | RUNNING     | 181             | 1          \s
                aareg-dist-api                                | prod-fss    | Application | RUNNING     | 181             | 2          \s
                aareg-dist-api                                | prod-gcp    | Application | RUNNING     | 15              | 1          \s
                aareg-dist-api-q2                             | dev-gcp     | Application | RUNNING     | 30              | 1          \s
                aareg-dist-mottak                             | dev-fss     | Application | RUNNING     | 29              | 1          \s
                aareg-dist-mottak                             | prod-fss    | Application | RUNNING     | 29              | 1          \s
                aareg-dist-mottak-q1                          | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dist-online                             | prod-gcp    | Application | RUNNING     | 33              | 1          \s
                aareg-dist-online-api                         | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dist-online-api                         | prod-fss    | Application | RUNNING     | 32              | 1          \s
                aareg-dist-online-api-q1                      | dev-fss     | Application | RUNNING     | 28              | 1          \s
                aareg-dist-online-api-q2                      | dev-fss     | Application | RUNNING     | 28              | 1          \s
                aareg-dist-online-gcp                         | dev-gcp     | Application | RUNNING     | 33              | 1          \s
                aareg-dist-online-q1                          | dev-gcp     | Application | RUNNING     | 29              | 1          \s
                aareg-dist-online-q1-frontend                 | dev-gcp     | Application | RUNNING     | 30              | 1          \s
                aareg-dist-online-q2                          | dev-gcp     | Application | RUNNING     | 29              | 1          \s
                aareg-dist-online-q2-frontend                 | dev-gcp     | Application | RUNNING     | 30              | 1          \s
                aareg-dist-sync                               | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dist-sync                               | prod-fss    | Application | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer                     | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer                     | prod-fss    | Application | RUNNING     | 32              | 1          \s
                aareg-dist-sync-endringer-q1                  | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dist-sync-q1                            | dev-fss     | Application | RUNNING     | 32              | 1          \s
                aareg-dolly-api-q0                            | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-dolly-api-q1                            | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-dolly-api-q2                            | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-dolly-api-q4                            | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-dolly-api-q5                            | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-hendelser-test-q1                       | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-hendelser-test-q5                       | dev-fss     | Application | RUNNING     | 18              | 1          \s
                aareg-innsyn-arbeidsgiver                     | dev-gcp     | Application | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver                     | prod-gcp    | Application | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver-api                 | dev-fss     | Application | RUNNING     | 29              | 1          \s
                aareg-innsyn-arbeidsgiver-api                 | prod-fss    | Application | RUNNING     | 29              | 1          \s
                aareg-maskinporten-token                      | dev-fss     | Application | RUNNING     | 16              | 1          \s
                aareg-mottak-arbeidsforhold                   | prod-fss    | Application | RUNNING     | 23              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser         | prod-fss    | Application | RUNNING     | 23              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser-q1      | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold-hendelser-q5      | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold-q1                | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-mottak-arbeidsforhold-q4                | dev-fss     | Application | RUNNING     | 29              | 1          \s
                aareg-mottak-arbeidsforhold-q5                | dev-fss     | Application | RUNNING     | 24              | 1          \s
                aareg-mottak-hendelser-vakt                   | prod-fss    | Job         | COMPLETED   | 27              | 1          \s
                aareg-mottak-opptjeningsgrunnlag              | prod-fss    | Application | RUNNING     | 23              | 1          \s
                aareg-mottak-opptjeningsgrunnlag-hendelser    | prod-fss    | Application | RUNNING     | 23              | 1          \s
                aareg-mottak-opptjeningsgrunnlag-hendelser-q4 | dev-fss     | Application | RUNNING     | 29              | 1          \s
                aareg-mottak-opptjeningsgrunnlag-q4           | dev-fss     | Application | RUNNING     | 29              | 1          \s
                aareg-oppdater-jurjur-hendelser-q4            | dev-fss     | Job         | FAILED      | 25              | 2          \s
                aareg-oppdater-jurjur-hendelser-q5            | dev-fss     | Job         | FAILED      | 25              | 2          \s
                aareg-otp-api                                 | dev-fss     | Application | RUNNING     | 177             | 2          \s
                aareg-otp-api                                 | dev-gcp     | Application | RUNNING     | 15              | 1          \s
                aareg-otp-api                                 | prod-fss    | Application | RUNNING     | 177             | 2          \s
                aareg-otp-api                                 | prod-gcp    | Application | RUNNING     | 15              | 1          \s
                aareg-otp-api-q2                              | dev-gcp     | Application | RUNNING     | 15              | 1          \s
                aareg-patch-funk-endret                       | prod-fss    | Job         | FAILED      | 27              | 2          \s
                aareg-services-nais                           | dev-fss     | Application | RUNNING     | 30              | 2          \s
                aareg-services-nais                           | prod-fss    | Application | RUNNING     | 30              | 1          \s
                aareg-services-nais-q0                        | dev-fss     | Application | RUNNING     | 30              | 2          \s
                aareg-services-nais-q1                        | dev-fss     | Application | RUNNING     | 27              | 2          \s
                aareg-services-nais-q4                        | dev-fss     | Application | RUNNING     | 15              | 2          \s
                aareg-services-nais-q5                        | dev-fss     | Application | RUNNING     | 27              | 2          \s
                aareg-status                                  | dev-gcp     | Application | RUNNING     | 1               | 0          \s
                aareg-status                                  | prod-gcp    | Application | RUNNING     | 1               | 0          \s
                aareg-tenor-adapter                           | dev-gcp     | Application | RUNNING     | 35              | 1          \s
                aareg-tilgangskontroll                        | prod-gcp    | Application | RUNNING     | 17              | 1          \s
                aareg-tilgangskontroll-q1                     | dev-gcp     | Application | RUNNING     | 17              | 1          \s
                aareg-tilgangskontroll-q2                     | dev-gcp     | Application | RUNNING     | 17              | 1          \s
                aareg-to-bq                                   | prod-fss    | Job         | COMPLETED   | 1               | 0          \s
                aareg-uttrekk-dhuk                            | prod-fss    | Job         | UNKNOWN     | 18              | 1          \s
                baareg004                                     | dev-fss     | Job         | COMPLETED   | 4               | 0          \s
                baareg004                                     | prod-fss    | Job         | COMPLETED   | 4               | 0          \s
                baareg010                                     | dev-fss     | Job         | RUNNING     | 3               | 0          \s
                baareg010                                     | prod-fss    | Job         | COMPLETED   | 35              | 1          \s
                baareg013-konkurser                           | prod-fss    | Job         | COMPLETED   | 3               | 0          \s
                demo-aareg-innsyn-arbeidsgiver                | dev-gcp     | Application | RUNNING     | 29              | 1          \s
                ereg-aura-runner                              | dev-fss     | Application | NOT_RUNNING | 0               | 2          \s
                ereg-avansert-sok                             | dev-fss     | Job         | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok                             | prod-fss    | Job         | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok-q0                          | dev-fss     | Job         | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok-q1                          | dev-fss     | Job         | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok-q4                          | dev-fss     | Job         | UNKNOWN     | 4               | 0          \s
                ereg-avansert-sok-q5                          | dev-fss     | Job         | UNKNOWN     | 4               | 0          \s
                ereg-services                                 | dev-fss     | Application | RUNNING     | 1               | 1          \s
                ereg-services                                 | prod-fss    | Application | RUNNING     | 1               | 1          \s
                ereg-services-q0                              | dev-fss     | Application | RUNNING     | 1               | 1          \s
                ereg-services-q1                              | dev-fss     | Application | RUNNING     | 1               | 1          \s
                ereg-services-q4                              | dev-fss     | Application | RUNNING     | 1               | 1          \s
                ereg-services-q5                              | dev-fss     | Application | RUNNING     | 1               | 1          \s
                team-arbeidsforhold-statistikk                | dev-gcp     | Application | RUNNING     | 1               | 0     \s
                """;
        return Arrays.stream(naisTabell.split("\n")).map(linje -> workload(linje.split(Pattern.quote("|")))).toList();
    }

    private Deployment workload(final String[] felt) {
        final DeploymentId id = new DeploymentId();
        id.setWorkloadName(felt[0].trim());
        id.setEnvironment(felt[1].trim());
        id.setWorkloadType(felt[2].trim());
        final Deployment deployment = new Deployment();
        deployment.setId(id);
        deployment.setLastSeen(Instant.now());
        return deployment;
    }

    public RepoDetails repoMedNavn(final String repoName) {
        return new RepoDetails(repoName, "https://github.com/" + repoName);
    }
}
