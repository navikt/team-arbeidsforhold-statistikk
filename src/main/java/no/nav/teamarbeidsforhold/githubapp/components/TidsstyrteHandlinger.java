package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("!local")
@ConditionalOnProperty(prefix = "oppdatering", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TidsstyrteHandlinger {
    private final LederUtpeker lederUtpeker;
    private final ObjectProvider<SlackSender> slackSender;
    private final KritiskeSårbarheterAnalyse analyse;
    private final KopierNvdCveData kopierNvdCveData;
    private final KopierNaisApiData kopierNaisApiData;

    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Oslo")
    public void oppdaterSårbarheterMedVarsel() {
        if (!lederUtpeker.erLeder()) {
            return;
        }
        try {
            log.info("Starter daglig NVD-import");
            kopierNvdCveData.kopierNvdCveDataTilDatabase().get(15, TimeUnit.MINUTES);
            log.info("NVD-import fullført; starter import av deployment- og sårbarhetsdata");
            kopierNaisApiData.kopierNaisApiDataTilDatabase().get(15, TimeUnit.MINUTES);
        } catch (final IOException | ExecutionException | TimeoutException | CancellationException e) {
            final Throwable feil = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            log.error("Daglig dataoppdatering feilet: {}. Sender ingen Slack-varsler",
                    feil.getClass().getSimpleName());
            return;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Daglig dataoppdatering ble avbrutt. Sender ingen Slack-varsler");
            return;
        }
        log.info("Daglig dataoppdatering fullført");
        final SlackSender sender = slackSender.getIfAvailable();
        if (sender != null) {
            analyse.sjekkSårbarheter();
            sender.sendKritiskSårbarhetsMeldinger();
        }
    }
}
