package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.qualifier.TrivyApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TrivyKonfigurasjon {
    public static final String TRIVY_ADRESSE = "http://team-arbeidsforhold-statistikk-trivy";

    @Bean
    @TrivyApi
    public WebClient trivyWebClient(final WebClient.Builder builder) {
        return builder.baseUrl(TRIVY_ADRESSE).build();
    }
}
