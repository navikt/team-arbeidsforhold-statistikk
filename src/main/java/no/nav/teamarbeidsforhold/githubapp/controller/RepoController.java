package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.RepoApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import no.nav.teamarbeidsforhold.githubapp.service.DatabaseService;
import no.nav.teamarbeidsforhold.githubapp.service.GithubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class RepoController implements RepoApi {
    private final GithubService githubService;
    private final DatabaseService databaseService;

    RepoController(final GithubService githubService, final DatabaseService databaseService) {
        this.githubService = githubService;
        this.databaseService = databaseService;
    }

    @Override
    public ResponseEntity<List<Repo>> apiRepoGet() {
        githubService.prøvOppdater();
        return ok(databaseService.alleRepoer());
    }

    @Override
    public ResponseEntity<RepoDetails> apiRepoRepoNameGet(final String repoName) {
        githubService.prøvOppdater();
        return ok(databaseService.repoMedNavn(repoName));
    }
}
