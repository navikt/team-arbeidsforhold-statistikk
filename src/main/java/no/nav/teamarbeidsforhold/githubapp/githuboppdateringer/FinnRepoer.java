package no.nav.teamarbeidsforhold.githubapp.githuboppdateringer;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.entity.Repo;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import no.nav.teamarbeidsforhold.githubapp.worker.GithubOppdatering;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public final class FinnRepoer implements GithubOppdatering {

    private final RepoRepository repository;

    public FinnRepoer(final RepoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void kjør(final GitHub api) throws IOException {
        final Map<String, Repo> gamle = repository.findAll().stream().collect(Collectors.groupingBy(Repo::getFullName, HashMap::new, Collectors.collectingAndThen(Collectors.reducing((_, _) -> {
            throw new IllegalArgumentException("Skal være kun ett repo per navn");
        }), optional -> optional.orElseThrow(IllegalArgumentException::new))));
        final Map<String, GHRepository> nye = new HashMap<>();
        for (final GHRepository repo : api.getOrganization("navikt").getTeamBySlug("arbeidsforhold").listRepositories()) {
            nye.put(repo.getFullName(), repo);
        }
        final Instant nå = Instant.now();
        gamle.forEach((navn, repo) -> {
            if (nye.containsKey(navn)) {
                final GHRepository nyeData = nye.get(repo.getFullName());
                if (!Objects.equals(repo.getCloneUrl(), nyeData.getHttpTransportUrl())) {
                    repo.setCloneUrl(nyeData.getHttpTransportUrl());
                    repository.save(repo);
                }
            } else {
                log.warn("Repo {} er slettet (eller vi har ikke lenger tilgang til det), men vi har data om det i databasen," +
                        " og ingen funksjonalitet for å slette eller skjule data som ikke lenger er relevant.", repo.getFullName());
            }
        });
        final List<Repo> lagNyeRepo = new  ArrayList<>();
        nye.forEach((repo, gitRepo) -> {
            if (!gamle.containsKey(repo)) {
                final Repo nyttRepo = new Repo();
                nyttRepo.setCloneUrl(gitRepo.getHttpTransportUrl());
                nyttRepo.setFullName(gitRepo.getFullName());
                nyttRepo.setLastSeen(nå);
                lagNyeRepo.add(nyttRepo);
            }
        });
        repository.saveAll(lagNyeRepo);
    }
}
