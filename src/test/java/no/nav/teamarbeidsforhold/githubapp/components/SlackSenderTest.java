package no.nav.teamarbeidsforhold.githubapp.components;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.JsonSyntaxException;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNoticeId;
import no.nav.teamarbeidsforhold.githubapp.properties.SlackInnstillinger;
import no.nav.teamarbeidsforhold.githubapp.repository.CriticalVulnerabilityNoticeRepository;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SlackSenderTest {
    private static final Instant NÅ = Instant.parse("2026-11-24T11:00:00Z");
    private static final String HEMMELIGHET = "hemmelig-testverdi";

    private final MethodsClient slackMethods = mock(MethodsClient.class);
    private final CriticalVulnerabilityNoticeRepository repository = mock(CriticalVulnerabilityNoticeRepository.class);
    private final Clock klokke = mock(Clock.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AtomicReference<Instant> tidspunkt = new AtomicReference<>(NÅ);
    private final CriticalVulnerabilityNoticeId id = new CriticalVulnerabilityNoticeId();
    private final ListAppender<ILoggingEvent> logger = new ListAppender<>();
    private final Logger senderLogger = (Logger) LoggerFactory.getLogger(SlackSender.class);
    private final SlackSender sender = new SlackSender(
            new SlackInnstillinger(HEMMELIGHET, "#testkanal"), slackMethods, repository, klokke, meterRegistry);

    @BeforeEach
    void oppsett() throws Exception {
        id.setVulnerabilityId("CVE-2026-12345");
        id.setChannel("slack");
        when(klokke.instant()).thenAnswer(invocation -> tidspunkt.get());
        when(repository.findPendingSlackNotices(any())).thenReturn(List.of(id));
        when(repository.reserveForSending(eq(id), any(), eq("slack-bot"))).thenReturn(1, 0);
        when(repository.releaseForRetry(eq(id), any(), eq("slack-bot"))).thenReturn(1);
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(svar(true, null));
        logger.start();
        senderLogger.addAppender(logger);
    }

    @AfterEach
    void fjernLogger() {
        senderLogger.detachAppender(logger);
        logger.stop();
        meterRegistry.close();
    }

    @Test
    void reserverer_før_sending_og_sender_ikke_samme_varsel_igjen() throws Exception {
        sender.sendKritiskSårbarhetsMeldinger();
        sender.sendKritiskSårbarhetsMeldinger();

        final ArgumentCaptor<ChatPostMessageRequest> melding = ArgumentCaptor.forClass(ChatPostMessageRequest.class);
        final InOrder rekkefolge = inOrder(repository, slackMethods);
        rekkefolge.verify(repository).findPendingSlackNotices(NÅ);
        rekkefolge.verify(repository).reserveForSending(id, NÅ, "slack-bot");
        rekkefolge.verify(slackMethods).chatPostMessage(melding.capture());
        verify(slackMethods, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(melding.getValue().getChannel()).isEqualTo("#testkanal");
        assertThat(melding.getValue().getText())
                .contains("CVE-2026-12345 (CRITICAL)", "https://nvd.nist.gov/vuln/detail/CVE-2026-12345");
        assertThat(melding.getValue().isLinkNames()).isFalse();
        assertThat(melding.getValue().isUnfurlLinks()).isFalse();
        assertThat(melding.getValue().isUnfurlMedia()).isFalse();
        assertThat(loggtekst()).contains("bekreftet sendt");
        assertThat(meterRegistry.get("slack.varsler").tag("utfall", "bekreftet").counter().count()).isEqualTo(1);
    }

    @Test
    void usikkert_transportutfall_logges_uten_detaljer_og_forblir_registrert() throws Exception {
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenThrow(new IOException(HEMMELIGHET));

        sender.sendKritiskSårbarhetsMeldinger();
        sender.sendKritiskSårbarhetsMeldinger();

        verify(slackMethods, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(loggtekst()).contains("Usikker Slack-levering", "IOException").doesNotContain(HEMMELIGHET);
        assertThat(meterRegistry.get("slack.varsler").tag("utfall", "usikker").counter().count()).isEqualTo(1);
    }

    @Test
    void ugyldig_json_behandles_som_usikker_levering() throws Exception {
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenThrow(new JsonSyntaxException(HEMMELIGHET));

        assertDoesNotThrow(sender::sendKritiskSårbarhetsMeldinger);

        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(loggtekst()).contains("Usikker Slack-levering").doesNotContain(HEMMELIGHET);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sender_bare_ett_varsel_per_kjøring_også_ved_ukjent_svar(final boolean bekreftet) throws Exception {
        final CriticalVulnerabilityNoticeId nesteId = new CriticalVulnerabilityNoticeId();
        nesteId.setVulnerabilityId("CVE-2026-54321");
        nesteId.setChannel("slack");
        when(repository.findPendingSlackNotices(any())).thenReturn(List.of(id, nesteId));
        when(repository.reserveForSending(eq(nesteId), any(), eq("slack-bot"))).thenReturn(1);
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class)))
                .thenReturn(svar(bekreftet, bekreftet ? null : "internal_error"));

        sender.sendKritiskSårbarhetsMeldinger();

        verify(slackMethods, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
        verify(repository, never()).reserveForSending(eq(nesteId), any(), any());
        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(meterRegistry.get("slack.varsler").tag("utfall", bekreftet ? "bekreftet" : "usikker")
                .counter().count()).isEqualTo(1);

        tidspunkt.set(NÅ.plusSeconds(60));
        sender.sendKritiskSårbarhetsMeldinger();

        final ArgumentCaptor<ChatPostMessageRequest> meldinger = ArgumentCaptor.forClass(ChatPostMessageRequest.class);
        verify(slackMethods, times(2)).chatPostMessage(meldinger.capture());
        assertThat(meldinger.getAllValues().get(1).getText()).contains("CVE-2026-54321");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"internal_error", HEMMELIGHET})
    void ukjente_slack_feil_frigir_ikke_reservasjonen(final String feilkode) throws Exception {
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(svar(false, feilkode));

        sender.sendKritiskSårbarhetsMeldinger();

        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(loggtekst()).contains("antar sendt uten nytt forsøk").doesNotContain(HEMMELIGHET);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not_in_channel", "invalid_auth", "ratelimited"})
    void sikker_avvisning_kan_prøves_igjen_etter_tretti_minutter(final String feilkode) throws Exception {
        when(repository.reserveForSending(eq(id), any(), eq("slack-bot"))).thenReturn(1);
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class)))
                .thenReturn(svar(false, feilkode), svar(true, null));

        sender.sendKritiskSårbarhetsMeldinger();
        tidspunkt.set(NÅ.plusSeconds(1799));
        sender.sendKritiskSårbarhetsMeldinger();
        verify(slackMethods, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
        tidspunkt.set(NÅ.plusSeconds(1800));
        sender.sendKritiskSårbarhetsMeldinger();

        verify(repository).releaseForRetry(id, NÅ, "slack-bot");
        verify(slackMethods, times(2)).chatPostMessage(any(ChatPostMessageRequest.class));
        assertThat(meterRegistry.get("slack.varsler").tag("utfall", "sikker_avvisning").counter().count()).isEqualTo(1);
    }

    @Test
    void ratebegrensning_stopper_hele_kjøringen_og_respekterer_retry_after() throws Exception {
        final CriticalVulnerabilityNoticeId nesteId = new CriticalVulnerabilityNoticeId();
        nesteId.setVulnerabilityId("CVE-2026-54321");
        nesteId.setChannel("slack");
        when(repository.findPendingSlackNotices(any())).thenReturn(List.of(id, nesteId));
        when(repository.reserveForSending(eq(id), any(), eq("slack-bot"))).thenReturn(1);
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class)))
                .thenThrow(httpFeil(429, "7200")).thenReturn(svar(true, null));

        sender.sendKritiskSårbarhetsMeldinger();
        tidspunkt.set(NÅ.plusSeconds(7199));
        sender.sendKritiskSårbarhetsMeldinger();

        verify(repository, times(1)).findPendingSlackNotices(any());
        verify(repository, never()).reserveForSending(eq(nesteId), any(), any());
        verify(repository).releaseForRetry(id, NÅ, "slack-bot");

        tidspunkt.set(NÅ.plusSeconds(7200));
        sender.sendKritiskSårbarhetsMeldinger();
        verify(slackMethods, times(2)).chatPostMessage(any(ChatPostMessageRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ugyldig", "0", "-1"})
    void ugyldig_retry_after_logges_og_gir_tretti_minutters_pause(final String retryAfter) throws Exception {
        when(repository.reserveForSending(eq(id), any(), eq("slack-bot"))).thenReturn(1);
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class)))
                .thenThrow(httpFeil(429, retryAfter)).thenReturn(svar(true, null));

        sender.sendKritiskSårbarhetsMeldinger();
        tidspunkt.set(NÅ.plusSeconds(1799));
        sender.sendKritiskSårbarhetsMeldinger();
        verify(slackMethods, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
        tidspunkt.set(NÅ.plusSeconds(1800));
        sender.sendKritiskSårbarhetsMeldinger();
        verify(slackMethods, times(2)).chatPostMessage(any(ChatPostMessageRequest.class));
        assertThat(loggtekst()).contains("Retry-After");
    }

    @Test
    void serverfeil_forblir_antatt_sendt() throws Exception {
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenThrow(httpFeil(503, null));

        sender.sendKritiskSårbarhetsMeldinger();

        verify(repository, never()).releaseForRetry(any(), any(), any());
        assertThat(loggtekst()).contains("HTTP 503", "antar sendt").doesNotContain(HEMMELIGHET);
    }

    @Test
    void databasefeil_før_sending_hindrer_slack_kall() {
        when(repository.reserveForSending(eq(id), any(), any()))
                .thenThrow(new DataAccessResourceFailureException(HEMMELIGHET));

        sender.sendKritiskSårbarhetsMeldinger();

        verifyNoInteractions(slackMethods);
        assertThat(loggtekst()).contains("Usikker reservasjon").doesNotContain(HEMMELIGHET);
    }

    @Test
    void databasefeil_ved_frigivelse_beholder_antatt_sending_og_logges() throws Exception {
        when(slackMethods.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(svar(false, "invalid_auth"));
        when(repository.releaseForRetry(eq(id), any(), any()))
                .thenThrow(new DataAccessResourceFailureException(HEMMELIGHET));

        sender.sendKritiskSårbarhetsMeldinger();

        assertThat(loggtekst()).contains("Kunne ikke frigi", "antar sendt").doesNotContain(HEMMELIGHET);
    }

    @Test
    void validerer_cve_id_uten_å_logge_ugyldig_innhold() {
        id.setVulnerabilityId("<!channel>" + HEMMELIGHET);

        sender.sendKritiskSårbarhetsMeldinger();

        verify(repository, never()).reserveForSending(any(), any(), any());
        verifyNoInteractions(slackMethods);
        assertThat(loggtekst()).contains("ugyldig CVE-ID").doesNotContain(HEMMELIGHET, "<!channel>");
    }

    private String loggtekst() {
        return String.join("\n", logger.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private static ChatPostMessageResponse svar(final boolean ok, final String feilkode) {
        final ChatPostMessageResponse svar = new ChatPostMessageResponse();
        svar.setOk(ok);
        svar.setError(feilkode);
        return svar;
    }

    private static SlackApiException httpFeil(final int status, final String retryAfter) {
        final Response.Builder respons = new Response.Builder()
                .request(new Request.Builder().url("https://slack.com/api/chat.postMessage").build())
                .protocol(Protocol.HTTP_1_1).code(status).message("Testfeil");
        if (retryAfter != null) {
            respons.header("Retry-After", retryAfter);
        }
        return new SlackApiException(respons.build(), "{\"error\":\"" + HEMMELIGHET + "\"}");
    }
}
