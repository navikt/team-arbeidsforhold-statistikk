package no.nav.teamarbeidsforhold.githubapp;

import no.nav.teamarbeidsforhold.githubapp.components.*;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NaisApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest
@TestPropertySource(properties = {
        "NAIS_SERVICE_ACCOUNT_TOKEN_PATH=src/test/resources/naistoken"
})
@Import({TestKeysConfig.class})
class TeamArbeidsforholdStatistikkApplicationTests {
    @Autowired
    TrivyKjører trivyKjører;
    @Autowired
    GithubOppdateringsKø githubOppdateringsKø;
    @Autowired
    Lagring lagring;
    @Autowired
    KopierNaisApiData kopierNaisApiData;
    @Autowired
    KopierNvdCveData kopierNvdCveData;

    @Test
    void contextLoads() {
    }
}
