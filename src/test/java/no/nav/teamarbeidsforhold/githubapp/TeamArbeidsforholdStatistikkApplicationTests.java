package no.nav.teamarbeidsforhold.githubapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestKeysConfig.class})
class TeamArbeidsforholdStatistikkApplicationTests {

    @Test
    void contextLoads() {
    }

}
