package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.Workload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkloadRepository extends JpaRepository<Workload, String> {
}
