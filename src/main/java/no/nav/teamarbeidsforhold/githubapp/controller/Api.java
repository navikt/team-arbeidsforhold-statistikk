package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.ReposApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.service.GithubService;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class Api implements ReposApi {
    private final GithubService githubService;

    Api(final GitHub github, final GithubService githubService) {
        this.githubService = githubService;
    }

    public ResponseEntity<List<Repo>> reposGet() {
        try {
            githubService.prøvOppdater();

            return ResponseEntity.ok(repoer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<GHRepository> prøvOppdater() throws IOException {
        return githubService.prøvOppdater();
    }
}
