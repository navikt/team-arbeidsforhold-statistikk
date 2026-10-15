package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.RequiredArgsConstructor;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNotice;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNoticeId;
import no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability;
import no.nav.teamarbeidsforhold.githubapp.repository.CriticalVulnerabilityNoticeRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class KritiskeSårbarheterAnalyse {
    private final CriticalVulnerabilityNoticeRepository criticalVulnerabilityNoticeRepository;
    private final DeploymentVulnerabilityRepository deploymentVulnerabilityRepository;

    private static CriticalVulnerabilityNotice lagMelding(final Vulnerability vulnerability) {
        final CriticalVulnerabilityNotice sårbarhet = new CriticalVulnerabilityNotice();
        final CriticalVulnerabilityNoticeId id = new CriticalVulnerabilityNoticeId();
        sårbarhet.setVulnerability(vulnerability);
        id.setVulnerabilityId(vulnerability.getId());
        id.setChannel("slack");
        sårbarhet.setId(id);
        sårbarhet.setFirstNoticed(Instant.now());
        return sårbarhet;
    }

    @Transactional
    public void sjekkSårbarheter() {
        criticalVulnerabilityNoticeRepository.saveAll(
                deploymentVulnerabilityRepository.findDeployedWithoutNotice("CRITICAL")
                        .stream().map(KritiskeSårbarheterAnalyse::lagMelding).toList());
    }
}
