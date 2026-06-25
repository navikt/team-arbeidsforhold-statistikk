package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.CveNvdMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CveNvdMetaRepository extends JpaRepository<CveNvdMeta, String> {
    Optional<CveNvdMeta> findTopByOrderByTimestampDesc();
}
