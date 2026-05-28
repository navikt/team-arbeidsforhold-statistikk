package no.nav.teamarbeidsforhold.githubapp;

import no.nav.teamarbeidsforhold.githubapp.components.GithubOppdateringsKø;
import no.nav.teamarbeidsforhold.githubapp.components.Lagring;
import no.nav.teamarbeidsforhold.githubapp.components.TrivyKjører;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest
@Import({TestKeysConfig.class})
class TeamArbeidsforholdStatistikkApplicationTests {
    @Autowired
    TrivyKjører trivyKjører;
    @Autowired
    GithubOppdateringsKø githubOppdateringsKø;
    @Autowired
    Lagring lagring;

    @Test
    void contextLoads() {
    }
}
