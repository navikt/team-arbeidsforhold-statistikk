package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.RepoApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import no.nav.teamarbeidsforhold.githubapp.githuboppdateringer.FinnRepoer;
import no.nav.teamarbeidsforhold.githubapp.components.Lagring;
import no.nav.teamarbeidsforhold.githubapp.components.GithubOppdateringsKø;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class RepoController implements RepoApi {
    private final GithubOppdateringsKø githubOppdateringsKø;
    private final Lagring lagring;
    private final FinnRepoer finnRepoerOppdatering;

    RepoController(final GithubOppdateringsKø githubOppdateringsKø, final Lagring lagring, final FinnRepoer finnRepoerOppdatering) {
        this.githubOppdateringsKø = githubOppdateringsKø;
        this.lagring = lagring;
        this.finnRepoerOppdatering = finnRepoerOppdatering;
    }

    @Override
    public ResponseEntity<List<Repo>> apiRepoGet() {
        githubOppdateringsKø.oppdater(finnRepoerOppdatering);
        return ok(lagring.alleRepoer());
    }

    @Override
    public ResponseEntity<RepoDetails> apiRepoRepoNameGet(final String repoName) {
        return ok(lagring.repoMedNavn(repoName));
    }
}
