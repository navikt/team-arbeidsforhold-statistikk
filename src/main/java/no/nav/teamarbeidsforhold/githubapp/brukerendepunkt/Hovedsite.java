package no.nav.teamarbeidsforhold.githubapp.brukerendepunkt;

import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/")
class Hovedsite {
    private final GitHub github;

    Hovedsite(final GitHub github) {
        this.github = github;
    }

    @GetMapping("/repoer")
    public ResponseEntity<?> get() throws IOException {
        github.getMyTeams();
        return ResponseEntity.ok("repoer");
    }
}
