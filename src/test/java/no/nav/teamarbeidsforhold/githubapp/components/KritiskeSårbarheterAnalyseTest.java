package no.nav.teamarbeidsforhold.githubapp.components;

import jakarta.persistence.EntityManager;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNotice;
import no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentVulnerabilityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KritiskeSårbarheterAnalyseTest {
    @Test
    void oppretter_nye_varsler_uten_å_overskrive_eksisterende() {
        final Instant nå = Instant.parse("2026-11-24T11:00:00Z");
        final DeploymentVulnerabilityRepository repository = mock(DeploymentVulnerabilityRepository.class);
        final EntityManager entityManager = mock(EntityManager.class);
        final Vulnerability sårbarhet = new Vulnerability();
        sårbarhet.setId("CVE-2026-12345");
        when(repository.findDeployedWithoutNotice("CRITICAL", nå.minus(Duration.ofHours(36)), nå))
                .thenReturn(List.of(sårbarhet));
        final KritiskeSårbarheterAnalyse analyse = new KritiskeSårbarheterAnalyse(
                entityManager, repository, Clock.fixed(nå, ZoneOffset.UTC));

        analyse.sjekkSårbarheter();

        final ArgumentCaptor<CriticalVulnerabilityNotice> varsel = ArgumentCaptor.forClass(CriticalVulnerabilityNotice.class);
        verify(entityManager).persist(varsel.capture());
        verifyNoMoreInteractions(entityManager);
        assertThat(varsel.getValue().getId().getVulnerabilityId()).isEqualTo(sårbarhet.getId());
        assertThat(varsel.getValue().getId().getChannel()).isEqualTo("slack");
        assertThat(varsel.getValue().getFirstNoticed()).isEqualTo(nå);
        assertThat(varsel.getValue().getWhenSent()).isNull();
    }
}
