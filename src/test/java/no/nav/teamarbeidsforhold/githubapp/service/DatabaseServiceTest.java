package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.entity.DeploymentId;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Repo;
import no.nav.teamarbeidsforhold.githubapp.repository.RepoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {
    @Mock
    RepoRepository repoRepository;
    @InjectMocks
    DatabaseService databaseService;

    @Test
    void alleDeploymentsInneholderRepo() {
        final List<Deployment> deployments = databaseService.alleDeployments();
        final List<Repo> repos = databaseService.alleRepoer();
        assertFalse(deployments.isEmpty());
        final Map<Boolean, List<Deployment>> split = deployments.stream().collect(Collectors.partitioningBy(deployment -> repos.stream().anyMatch(repo -> deployment.getId().getWorkloadName().startsWith(repo.getName()))));
        assertFalse(split.get(true).stream().map(Deployment::getId).map(DeploymentId::getWorkloadName).toList().isEmpty());
        assertFalse(split.get(false).stream().map(Deployment::getId).map(DeploymentId::getWorkloadName).toList().isEmpty());
        assertTrue(databaseService.repoMedNavn("ereg-services").getEnvironments().containsAll(List.of("dev-fss","prod-fss")));
    }
}