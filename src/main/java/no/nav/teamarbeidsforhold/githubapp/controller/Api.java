package no.nav.teamarbeidsforhold.githubapp.controller;

import jakarta.persistence.EntityManager;
import no.nav.teamarbeidsforhold.githubapp.generert.api.ReposApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.service.DatabaseService;
import no.nav.teamarbeidsforhold.githubapp.service.GithubService;
import org.kohsuke.github.GHRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class Api implements ReposApi {
    private final GithubService githubService;
    private final DatabaseService databaseService;

    Api(final GithubService githubService, final DatabaseService databaseService) {
        this.githubService = githubService;
        this.databaseService = databaseService;
    }

    public ResponseEntity<List<Repo>> reposGet() {
        githubService.prøvOppdater();
        return ResponseEntity.ok(databaseService.findAll());
    }
}
