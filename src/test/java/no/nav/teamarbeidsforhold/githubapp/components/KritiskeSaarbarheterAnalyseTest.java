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

class KritiskeSaarbarheterAnalyseTest {
    @Test
    void oppretterNyeVarslerUtenAaOverskriveEksisterende() {
        final Instant naa = Instant.parse("2026-11-24T11:00:00Z");
        final DeploymentVulnerabilityRepository repository = mock(DeploymentVulnerabilityRepository.class);
        final EntityManager entityManager = mock(EntityManager.class);
        final Vulnerability saarbarhet = new Vulnerability();
        saarbarhet.setId("CVE-2026-12345");
        when(repository.findDeployedWithoutNotice("CRITICAL", naa.minus(Duration.ofHours(36)), naa))
                .thenReturn(List.of(saarbarhet));
        final KritiskeSårbarheterAnalyse analyse = new KritiskeSårbarheterAnalyse(
                entityManager, repository, Clock.fixed(naa, ZoneOffset.UTC));

        analyse.sjekkSårbarheter();

        final ArgumentCaptor<CriticalVulnerabilityNotice> varsel = ArgumentCaptor.forClass(CriticalVulnerabilityNotice.class);
        verify(entityManager).persist(varsel.capture());
        verifyNoMoreInteractions(entityManager);
        assertThat(varsel.getValue().getId().getVulnerabilityId()).isEqualTo(saarbarhet.getId());
        assertThat(varsel.getValue().getId().getChannel()).isEqualTo("slack");
        assertThat(varsel.getValue().getFirstNoticed()).isEqualTo(naa);
        assertThat(varsel.getValue().getWhenSent()).isNull();
    }
}
