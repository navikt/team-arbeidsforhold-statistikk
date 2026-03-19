package no.nav.teamarbeidsforhold.githubapp.brukerendepunkt;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
class Hovedsite {
    private final GitHub github;

    Hovedsite(final GitHub github) {
        this.github = github;
    }

    @GetMapping("/repoer")
    public ResponseEntity<?> get() throws IOException {
        final GHTeam team = github.getMyTeams().values().stream().findAny().orElseThrow().stream().findAny().orElseThrow();
        final List<GHRepository> repoer = team.listRepositories().toList();
        return ResponseEntity.ok("repoer: "+repoer.stream().map(GHRepository::getName).collect(Collectors.joining(", ")));
    }
}
