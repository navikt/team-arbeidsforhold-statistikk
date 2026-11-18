package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.entity.DeploymentId;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.RepoDetails;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Component
@Transactional
public class Lagring {
    private final RepoRepository repoRepository;
    private final DeploymentRepository deploymentRepository;

    public Lagring(final RepoRepository repoRepository, final DeploymentRepository deploymentRepository) {
        this.repoRepository = repoRepository;
        this.deploymentRepository = deploymentRepository;
    }

    public List<Repo> alleRepoer() {
        return repoRepository.findAll().stream().map(repoEntitet -> new Repo(repoEntitet.getFullName(), URI.create(repoEntitet.getCloneUrl()), 0, 0)).toList();
    }

    public List<Deployment> alleDeployments() {
        return deploymentRepository.findAll();
    }

    public RepoDetails repoMedNavn(final String repoName) {
        final List<Deployment> mineDeployments = alleDeployments().stream().filter(d -> d.getId().getWorkloadName().equals(repoName) || d.getId().getWorkloadName().startsWith(repoName) && d.getId().getWorkloadName().substring(repoName.length()).matches("-\\w\\d")).toList();
        return new RepoDetails(repoName, URI.create("https://github.com/" + repoName), 0, mineDeployments.size() - 2, mineDeployments.stream().map(Deployment::getId).map(DeploymentId::getWorkloadName).distinct().toList(), mineDeployments.stream().map(Deployment::getId).map(DeploymentId::getEnvironment).distinct().toList(), !mineDeployments.isEmpty());
    }
}
