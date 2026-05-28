package no.nav.teamarbeidsforhold.githubapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TrivyKonfigurasjon {
    public static final String TRIVY_ADDRESE = "http://team-arbeidsforhold-statistikk-trivy";

    @Bean
    public WebClient webClient(final WebClient.Builder builder) {
        return builder.baseUrl(TRIVY_ADDRESE).build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
