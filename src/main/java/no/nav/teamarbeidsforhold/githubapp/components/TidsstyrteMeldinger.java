package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class TidsstyrteMeldinger {
    private final LederUtpeker lederUtpeker;
    private final SlackSender slackSender;
    private final KritiskeSårbarheterAnalyse analyse;

    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 30)
    public void sjekkOgSendMelding() {
        if (lederUtpeker.erLeder()) {
            analyse.sjekkSårbarheter();
            slackSender.sendMeldinger();
        }
    }
}
