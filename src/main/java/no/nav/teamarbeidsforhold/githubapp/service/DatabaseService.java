package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class DatabaseService {
    private final RepoRepository repoRepository;

    public DatabaseService(final RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    public List<Repo> alleRepoer() {
        final String repoer = """
                navikt/aareg
                navikt/aareg-ajourholdsfiler
                navikt/aareg-aktor-sync
                navikt/aareg-analyse
                navikt/aareg-aura
                navikt/aareg-batch
                navikt/aareg-dist-api
                navikt/aareg-dist-api-contract
                navikt/aareg-dist-mottak
                navikt/aareg-dist-mottak-contract
                navikt/aareg-dist-online
                navikt/aareg-dist-online-api
                navikt/aareg-dist-sync
                navikt/aareg-dist-sync-endringer
                navikt/aareg-dolly-api
                navikt/aareg-gamle-arbeidsgivernavn
                navikt/aareg-innsyn-saksbehandler
                navikt/aareg-melding-pusher
                navikt/aareg-meldingsbehandling
                navikt/aareg-meldingsdefinisjon
                navikt/aareg-metrics-pusher
                navikt/aareg-otp-api
                navikt/aareg-services
                navikt/aareg-services-contract
                navikt/aareg-status
                navikt/aareg-teknisk-historikk
                navikt/aareg-tenor-adapter
                navikt/aareg-test-container
                navikt/aareg-tilgangskontroll
                navikt/aareg-tjenestespesifikasjoner
                navikt/aareg-uttrekk
                navikt/aareg-uttrekk-eksternt
                navikt/aareg-vedlikehold
                navikt/abac-aareg-core
                navikt/abac-brreg-proxy
                navikt/abac-registre-aareg
                navikt/arbeid-og-inntekt
                navikt/arbeidsgiver-innsyn-aareg
                navikt/arbeidsgiver-innsyn-aareg-api
                navikt/brreg-proxy
                navikt/dataprodukt-register-aareg
                navikt/dataprodukt-register-ereg
                navikt/ereg-aura
                navikt/ereg-services
                navikt/ereg-solr-enhetssok
                navikt/nav-api-portal
                navikt/nav-maskinporten
                navikt/team-arbeidsforhold-github-runner
                navikt/team-arbeidsforhold-statistikk
                navikt/team-arbeidsforhold-utvikling
                navikt/vault-iac
                """;
        return Arrays.stream(repoer.split("\\s+")).map(navn -> new Repo(navn, "https://github.com/navikt/" + navn, 0)).toList();
        //return repoRepository.gyldigeRepoer().stream().map(repoEntitet -> new Repo(repoEntitet.getFullName(), repoEntitet.getCloneUrl(), 0)).toList();
    }
}
