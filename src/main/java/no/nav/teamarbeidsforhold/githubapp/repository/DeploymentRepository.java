package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, String> {
}
