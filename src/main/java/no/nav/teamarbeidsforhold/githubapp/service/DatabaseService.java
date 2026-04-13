package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class DatabaseService {
    private final RepoRepository repoRepository;

    public DatabaseService(final RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    public List<Repo> findAll() {
        return repoRepository.gyldigeRepoer().stream().map(repoEntitet -> new Repo(repoEntitet.getFullName(), repoEntitet.getCloneUrl(), 0)).toList();
    }
}
