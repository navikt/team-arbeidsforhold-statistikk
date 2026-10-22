package no.nav.teamarbeidsforhold.githubapp.components;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import lombok.RequiredArgsConstructor;
import no.nav.teamarbeidsforhold.githubapp.properties.SlackInnstillinger;
import no.nav.teamarbeidsforhold.githubapp.repository.CriticalVulnerabilityNoticeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SlackSender {
    private final SlackInnstillinger innstillinger;
    private final Slack slack;
    private final CriticalVulnerabilityNoticeRepository criticalVulnerabilityNoticeRepository;

    @Transactional
    public void sendKritiskSårbarhetsMeldinger() throws SlackApiException, IOException {
        criticalVulnerabilityNoticeRepository.findBy()
        sendMelding("#","hei");
    }

    public void sendMelding(final String kanal, final String melding) throws SlackApiException, IOException {
        slack.methods(innstillinger.token()).chatPostMessage(x->x.channel(kanal).text(melding));
    }
}
