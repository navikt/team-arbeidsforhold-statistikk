package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CveNvdRepository extends JpaRepository<CveNvd, String> {
}
