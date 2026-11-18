package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.components.Lagring;
import no.nav.teamarbeidsforhold.githubapp.generert.api.RepoApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Profile("local")
public class LokalRepoController implements RepoApi {
    private final Lagring lagring;

    public LokalRepoController(final Lagring lagring) {
        this.lagring = lagring;
    }

    @Override
    public ResponseEntity<List<Repo>> apiRepoGet() {
        return ok(lagring.alleRepoer());
    }

    @Override
    public ResponseEntity<RepoDetails> apiRepoRepoNameGet(final String repoName) {
        return ok(lagring.repoMedNavn(repoName));
    }
}
