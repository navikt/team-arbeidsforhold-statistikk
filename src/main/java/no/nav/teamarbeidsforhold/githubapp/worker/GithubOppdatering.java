package no.nav.teamarbeidsforhold.githubapp.worker;

import org.kohsuke.github.GitHub;

import java.io.IOException;

public interface GithubOppdatering {
    void kjør(GitHub api) throws IOException;
}
