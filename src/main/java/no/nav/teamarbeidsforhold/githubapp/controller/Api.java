package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.ReposApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class Api implements ReposApi {
    private final GitHub github;

    Api(final GitHub github) {
        this.github = github;
    }

    public ResponseEntity<List<Repo>> reposGet() {
        try {
            final GHTeam team = github.getMyTeams().values().stream().findAny().orElseThrow().stream().findAny().orElseThrow();
            final List<GHRepository> repoer = team.listRepositories().toList();
            return ResponseEntity.ok(repoer.stream().map(GHRepository::getName).map(r -> new Repo(r, "", 0)).toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
