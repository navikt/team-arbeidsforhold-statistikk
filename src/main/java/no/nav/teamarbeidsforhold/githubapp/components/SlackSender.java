package no.nav.teamarbeidsforhold.githubapp.components;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public final class SlackSender {
    private final LederUtpeker lederUtpeker;

    public SlackSender(final LederUtpeker lederUtpeker) {
        this.lederUtpeker = lederUtpeker;
    }

    @Scheduled(timeUnit = TimeUnit.MINUTES,fixedRate = 30)
    public void sjekkOgSendMelding() {
        if(lederUtpeker.erLeder()){
            skjekk
        }
    }
}
