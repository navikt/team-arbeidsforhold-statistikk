package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SlackSender {
    @Transactional
    public void sendMeldinger() {

    }
}
