package no.nav.teamarbeidsforhold.githubapp.components;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNotice;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNoticeId;
import no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Component
public class KritiskeSårbarheterAnalyse {
    static final Duration MAKSIMAL_CVE_ALDER = Duration.ofHours(36);

    private final EntityManager entityManager;
    private final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository;
    private final Clock klokke;

    private static CriticalVulnerabilityNotice lagMelding(final Vulnerability vulnerability, final Instant nå) {
        final CriticalVulnerabilityNotice sårbarhet = new CriticalVulnerabilityNotice();
        final CriticalVulnerabilityNoticeId id = new CriticalVulnerabilityNoticeId();
        sårbarhet.setVulnerability(vulnerability);
        id.setVulnerabilityId(vulnerability.getId());
        id.setChannel("slack");
        sårbarhet.setId(id);
        sårbarhet.setFirstNoticed(nå);
        return sårbarhet;
    }

    @Transactional
    public void sjekkSårbarheter() {
        final Instant nå = klokke.instant();
        for (final Vulnerability sårbarhet : deploymentVulnerabilityRepository.findDeployedWithoutNotice(
                "CRITICAL", nå.minus(MAKSIMAL_CVE_ALDER), nå)) {
            entityManager.persist(lagMelding(sårbarhet, nå));
        }
    }
}
