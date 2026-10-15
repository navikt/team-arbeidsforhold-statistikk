package no.nav.teamarbeidsforhold.githubapp.repository;

import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNotice;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNoticeId;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CveNvdRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CveNvdRepository repository;

    @Test
    void findWithoutNotice() {
        final Vulnerability vulnerability1 = em.persist(createVulnerability("CVE-1"));
        final Vulnerability vulnerability2 = em.persist(createVulnerability("CVE-2"));
        em.persist(createCritical(vulnerability1));
        em.persist(createCritical(vulnerability2));
        final CriticalVulnerabilityNoticeId id = new CriticalVulnerabilityNoticeId();
        final CriticalVulnerabilityNotice notice = new CriticalVulnerabilityNotice();
        notice.setVulnerability(vulnerability1);
        id.setVulnerabilityId(vulnerability1.getId());
        id.setChannel("slack");
        notice.setId(id);
        notice.setFirstNoticed(Instant.now());
        em.persist(notice);
        em.flush();

        final List<CveNvd> result = repository.findWithoutNotice("CRITICAL");

        assertThat(result).extracting(CveNvd::getVulnerability).containsExactly(vulnerability2);
    }

    private static CveNvd createCritical(final Vulnerability vulnerability1) {
        final CveNvd critical = new CveNvd();
        critical.setVulnerability(vulnerability1);
        critical.setBaseSeverity("CRITICAL");
        critical.setPublished(Instant.now());
        critical.setLastModified(Instant.now());
        return critical;
    }

    private static Vulnerability createVulnerability(final String id) {
        final Vulnerability vulnerability = new Vulnerability();
        vulnerability.setId(id);
        return vulnerability;
    }
}
