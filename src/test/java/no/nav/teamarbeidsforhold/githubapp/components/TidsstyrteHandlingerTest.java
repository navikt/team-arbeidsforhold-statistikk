package no.nav.teamarbeidsforhold.githubapp.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TidsstyrteHandlingerTest {
    private final LederUtpeker lederUtpeker = mock(LederUtpeker.class);
    private final SlackSender slackSender = mock(SlackSender.class);
    private final ObjectProvider<SlackSender> slackSenderProvider = mock();
    private final KritiskeSårbarheterAnalyse analyse = mock(KritiskeSårbarheterAnalyse.class);
    private final KopierNvdCveData nvd = mock(KopierNvdCveData.class);
    private final KopierNaisApiData deployment = mock(KopierNaisApiData.class);
    private final TidsstyrteHandlinger meldinger =
            new TidsstyrteHandlinger(lederUtpeker, slackSenderProvider, analyse, nvd, deployment);

    @Test
    void ikke_leder_skal_ikke_kalle_eksterne_integrasjoner() {
        meldinger.oppdaterSårbarheterMedVarsel();

        verifyNoInteractions(nvd, deployment, analyse, slackSenderProvider, slackSender);
    }

    @Test
    void leder_oppdaterer_data_og_analyserer_uten_sending() throws IOException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(slackSender);
        fullførImporter(true);

        meldinger.oppdaterSårbarheterMedVarsel();

        final InOrder rekkefolge = inOrder(nvd, deployment, analyse, slackSender);
        rekkefolge.verify(nvd).kopierNvdCveDataTilDatabase();
        rekkefolge.verify(deployment).kopierNaisApiDataTilDatabase();
        rekkefolge.verify(analyse).sjekkSårbarheter();
        verifyNoInteractions(slackSender);
    }

    @Test
    void uendret_nvd_data_forhindrer_ikke_andre_oppdateringer_eller_analyse() throws IOException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(slackSender);
        fullførImporter(false);

        meldinger.oppdaterSårbarheterMedVarsel();

        verify(deployment).kopierNaisApiDataTilDatabase();
        verify(analyse).sjekkSårbarheter();
        verifyNoInteractions(slackSender);
    }

    @Test
    void manglende_slack_sender_forhindrer_ikke_importene() throws IOException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(null);
        fullførImporter(true);

        meldinger.oppdaterSårbarheterMedVarsel();

        verify(nvd).kopierNvdCveDataTilDatabase();
        verify(deployment).kopierNaisApiDataTilDatabase();
        verifyNoInteractions(analyse, slackSender);
    }

    @Test
    void feil_ved_nvd_import_forhindrer_etterfølgende_import_og_slack() throws IOException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(nvd.kopierNvdCveDataTilDatabase())
                .thenReturn(CompletableFuture.failedFuture(new IOException("feil")));

        meldinger.oppdaterSårbarheterMedVarsel();

        verifyNoInteractions(deployment, analyse, slackSenderProvider, slackSender);
    }

    @Test
    void feil_ved_deployment_import_forhindrer_slack() throws IOException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(nvd.kopierNvdCveDataTilDatabase()).thenReturn(CompletableFuture.completedFuture(true));
        when(deployment.kopierNaisApiDataTilDatabase())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("feil")));

        meldinger.oppdaterSårbarheterMedVarsel();

        verifyNoInteractions(analyse, slackSenderProvider, slackSender);
    }

    @Test
    void venter_med_tidsavbrudd_på_begge_importene()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(slackSender);
        final CompletableFuture<Boolean> nvdResultat = mock();
        final CompletableFuture<Void> deploymentResultat = mock();
        when(nvd.kopierNvdCveDataTilDatabase()).thenReturn(nvdResultat);
        when(deployment.kopierNaisApiDataTilDatabase()).thenReturn(deploymentResultat);
        when(nvdResultat.get(15, TimeUnit.MINUTES)).thenReturn(true);
        when(deploymentResultat.get(15, TimeUnit.MINUTES)).thenReturn(null);

        meldinger.oppdaterSårbarheterMedVarsel();

        verify(nvdResultat).get(15, TimeUnit.MINUTES);
        verify(deploymentResultat).get(15, TimeUnit.MINUTES);
        verify(analyse).sjekkSårbarheter();
        verifyNoInteractions(slackSender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void tidsavbrudd_ved_en_av_importene_forhindrer_slack(final boolean nvdTimeout)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        final CompletableFuture<Boolean> nvdResultat = mock();
        final CompletableFuture<Void> deploymentResultat = mock();
        when(nvd.kopierNvdCveDataTilDatabase()).thenReturn(nvdResultat);
        if (nvdTimeout) {
            when(nvdResultat.get(15, TimeUnit.MINUTES)).thenThrow(new TimeoutException());
        } else {
            when(nvdResultat.get(15, TimeUnit.MINUTES)).thenReturn(true);
            when(deployment.kopierNaisApiDataTilDatabase()).thenReturn(deploymentResultat);
            when(deploymentResultat.get(15, TimeUnit.MINUTES)).thenThrow(new TimeoutException());
        }

        meldinger.oppdaterSårbarheterMedVarsel();

        verifyNoInteractions(analyse, slackSenderProvider, slackSender);
        if (nvdTimeout) {
            verifyNoInteractions(deployment);
        }
    }

    @Test
    void avbrutt_ventning_bevarer_avbruddsflagget()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        when(lederUtpeker.erLeder()).thenReturn(true);
        final CompletableFuture<Boolean> nvdResultat = mock();
        when(nvd.kopierNvdCveDataTilDatabase()).thenReturn(nvdResultat);
        when(nvdResultat.get(15, TimeUnit.MINUTES)).thenThrow(new InterruptedException());

        try {
            meldinger.oppdaterSårbarheterMedVarsel();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verifyNoInteractions(deployment, analyse, slackSenderProvider, slackSender);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void daglig_kjøring_er_planlagt_klokken_fem_i_oslo() throws NoSuchMethodException {
        final Scheduled plan = TidsstyrteHandlinger.class
                .getMethod("oppdaterSårbarheterMedVarsel")
                .getAnnotation(Scheduled.class);

        assertThat(plan).isNotNull();
        assertThat(plan.cron()).isEqualTo("0 0 5 * * *");
        assertThat(plan.zone()).isEqualTo("Europe/Oslo");
    }

    @Test
    void leder_sender_ventende_varsler_uten_import_eller_analyse() {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(slackSender);

        meldinger.sendVentendeSlackVarsler();

        verify(slackSender).sendKritiskSårbarhetsMeldinger();
        verifyNoInteractions(nvd, deployment, analyse);
    }

    @Test
    void ikke_leder_sender_ikke_ventende_varsler() {
        meldinger.sendVentendeSlackVarsler();

        verifyNoInteractions(slackSenderProvider, slackSender, nvd, deployment, analyse);
    }

    @Test
    void manglende_slack_sender_sender_ikke_ventende_varsler() {
        when(lederUtpeker.erLeder()).thenReturn(true);
        when(slackSenderProvider.getIfAvailable()).thenReturn(null);

        meldinger.sendVentendeSlackVarsler();

        verify(slackSenderProvider).getIfAvailable();
        verifyNoInteractions(slackSender, nvd, deployment, analyse);
    }

    @Test
    void ventende_varsler_planlegges_med_ett_minutts_forsinkelse() throws NoSuchMethodException {
        final Scheduled plan = TidsstyrteHandlinger.class
                .getMethod("sendVentendeSlackVarsler")
                .getAnnotation(Scheduled.class);

        assertThat(plan).isNotNull();
        assertThat(plan.fixedDelay()).isEqualTo(1);
        assertThat(plan.timeUnit()).isEqualTo(TimeUnit.MINUTES);
    }

    private void fullførImporter(final boolean nvdEndret) throws IOException {
        when(nvd.kopierNvdCveDataTilDatabase()).thenReturn(CompletableFuture.completedFuture(nvdEndret));
        when(deployment.kopierNaisApiDataTilDatabase()).thenReturn(CompletableFuture.completedFuture(null));
    }
}
