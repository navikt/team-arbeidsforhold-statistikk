package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CveNvdRepository extends JpaRepository<CveNvd, String> {
    @Query("""
    select c
    from CveNvd c
    where c.baseSeverity = :severity
      and not exists (
          select 1
          from CriticalVulnerabilityNotice n
          where n.vulnerability = c.vulnerability
      )
    """)
    List<CveNvd> findWithoutNotice(@Param("severity") String severity);
}
