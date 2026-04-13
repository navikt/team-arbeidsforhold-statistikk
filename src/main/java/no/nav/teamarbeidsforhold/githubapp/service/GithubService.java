package no.nav.teamarbeidsforhold.githubapp.service;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class GithubService {
    private final GitHub api;

    public GithubService(GitHub api) {
        this.api = api;
    }

    public void prøvOppdater() {
        try {
            final GHTeam team = api.getMyTeams().values().stream().findAny().orElseThrow().stream().findAny().orElseThrow();
            final List<GHRepository> repoer = team.listRepositories().toList();
            log.info("Fant repoer: {}", repoer);
        } catch (final IOException e) {
            log.warn("Kunne ikke lese fra github", e);
        }
    }
}