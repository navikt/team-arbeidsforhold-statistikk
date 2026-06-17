package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.CveNdvMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CveNdvMetaRepository extends JpaRepository<CveNdvMeta, String> {
    Optional<CveNdvMeta> findTopByOrderByTimestampDesc();
}
