package no.nav.teamarbeidsforhold.githubapp.components;

import com.google.gson.JsonParseException;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.entity.CriticalVulnerabilityNoticeId;
import no.nav.teamarbeidsforhold.githubapp.properties.SlackInnstillinger;
import no.nav.teamarbeidsforhold.githubapp.repository.CriticalVulnerabilityNoticeRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class SlackSender {
    private static final Pattern CVE_ID = Pattern.compile("CVE-[0-9]{4}-[0-9]{4,}");
    private static final String AVSENDER = "slack-bot";
    private static final Duration VENTETID_FØR_NYTT_FORSØK = Duration.ofMinutes(30);
    private static final Set<String> SIKRE_AVVISNINGER = Set.of(
            "channel_not_found", "not_in_channel", "is_archived", "invalid_auth", "not_authed",
            "token_revoked", "account_inactive", "missing_scope", "msg_too_long", "no_text",
            "invalid_arguments", "ratelimited");

    private final SlackInnstillinger innstillinger;
    private final MethodsClient slackMethods;
    private final CriticalVulnerabilityNoticeRepository criticalVulnerabilityNoticeRepository;
    private final Clock klokke;
    private final MeterRegistry meterRegistry;
    private Instant nesteForsøk = Instant.MIN;

    public synchronized void sendKritiskSårbarhetsMeldinger() {
        final Instant nå = klokke.instant();
        if (nå.isBefore(nesteForsøk)) {
            return;
        }
        for (final CriticalVulnerabilityNoticeId id : criticalVulnerabilityNoticeRepository.findPendingSlackNotices(nå)) {
            if (!CVE_ID.matcher(id.getVulnerabilityId()).matches()) {
                log.error("Slack-varsel hoppet over: ugyldig CVE-ID");
                continue;
            }
            final Instant forsøkstid = klokke.instant().truncatedTo(ChronoUnit.MICROS);
            log.info("Reserverer Slack-sendeforsøk for {}", id.getVulnerabilityId());
            try {
                if (criticalVulnerabilityNoticeRepository.reserveForSending(id, forsøkstid, AVSENDER) != 1) {
                    continue;
                }
            } catch (final DataAccessException | TransactionException e) {
                log.error("Usikker reservasjon av Slack-varsel for {}: {}, sender ikke",
                        id.getVulnerabilityId(), e.getClass().getSimpleName());
                return;
            }
            log.info("Slack-sendeforsøk registrert for {}", id.getVulnerabilityId());
            try {
                final ChatPostMessageResponse svar = slackMethods.chatPostMessage(ChatPostMessageRequest.builder()
                        .channel(innstillinger.channel())
                        .text("Kritisk sårbarhet: " + id.getVulnerabilityId() + " (CRITICAL)\n"
                                + "https://nvd.nist.gov/vuln/detail/" + id.getVulnerabilityId())
                        .linkNames(false)
                        .unfurlLinks(false)
                        .unfurlMedia(false)
                        .build());
                if (svar != null && svar.isOk()) {
                    meterRegistry.counter("slack.varsler", "utfall", "bekreftet").increment();
                    log.info("Slack-varsel bekreftet sendt for {}", id.getVulnerabilityId());
                } else if (svar != null && svar.getError() != null && SIKRE_AVVISNINGER.contains(svar.getError())) {
                    meterRegistry.counter("slack.varsler", "utfall", "sikker_avvisning").increment();
                    log.warn("Slack avviste varsel for {}: {}", id.getVulnerabilityId(), svar.getError());
                    nesteForsøk = klokke.instant().plus(VENTETID_FØR_NYTT_FORSØK);
                    frigiReservasjon(id, forsøkstid);
                    return;
                } else {
                    meterRegistry.counter("slack.varsler", "utfall", "usikker").increment();
                    log.error("Usikker Slack-levering for {}: ukjent svar, antar sendt uten nytt forsøk",
                            id.getVulnerabilityId());
                }
            } catch (final SlackApiException e) {
                if (e.getResponse().code() == 429) {
                    meterRegistry.counter("slack.varsler", "utfall", "sikker_avvisning").increment();
                    nesteForsøk = nesteTillatteForsøk(e.getResponse().header("Retry-After"));
                    log.warn("Slack begrenset senderaten for {}; neste forsøk tidligst {}",
                            id.getVulnerabilityId(), nesteForsøk);
                    frigiReservasjon(id, forsøkstid);
                } else {
                    meterRegistry.counter("slack.varsler", "utfall", "usikker").increment();
                    log.error("Usikker Slack-levering for {}: HTTP {}, antar sendt uten nytt forsøk",
                            id.getVulnerabilityId(), e.getResponse().code());
                }
                return;
            } catch (final IOException | JsonParseException e) {
                meterRegistry.counter("slack.varsler", "utfall", "usikker").increment();
                log.error("Usikker Slack-levering for {}: {}, antar sendt uten nytt forsøk",
                        id.getVulnerabilityId(), e.getClass().getSimpleName());
                return;
            }
            return;
        }
    }

    private void frigiReservasjon(final CriticalVulnerabilityNoticeId id, final Instant forsøkstid) {
        try {
            if (criticalVulnerabilityNoticeRepository.releaseForRetry(id, forsøkstid, AVSENDER) != 1) {
                log.error("Slack-reservasjon for {} ble ikke frigitt; antar sendt uten nytt forsøk",
                        id.getVulnerabilityId());
            }
        } catch (final DataAccessException | TransactionException e) {
            log.error("Kunne ikke frigi Slack-reservasjon for {}: {}, antar sendt uten nytt forsøk",
                    id.getVulnerabilityId(), e.getClass().getSimpleName());
        }
    }

    private Instant nesteTillatteForsøk(final String retryAfter) {
        if (retryAfter != null) {
            try {
                final long sekunder = Long.parseLong(retryAfter);
                if (sekunder > 0) {
                    try {
                        return klokke.instant().plusSeconds(sekunder);
                    } catch (final DateTimeException | ArithmeticException e) {
                        log.error("Slack krevde ventetid utenfor støttet tidsrom; stopper videre sendeforsøk");
                        return Instant.MAX;
                    }
                }
            } catch (final NumberFormatException e) {
                log.warn("Slack returnerte ugyldig Retry-After; venter 30 minutter");
                return klokke.instant().plus(VENTETID_FØR_NYTT_FORSØK);
            }
        }
        log.warn("Slack manglet gyldig Retry-After; venter 30 minutter");
        return klokke.instant().plus(VENTETID_FØR_NYTT_FORSØK);
    }
}
