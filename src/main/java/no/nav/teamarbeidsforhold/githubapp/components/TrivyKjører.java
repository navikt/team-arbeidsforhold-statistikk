package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.qualifier.TrivyApi;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public final class TrivyKjører {
    private final WebClient webClient;

    public TrivyKjører(@TrivyApi final WebClient webClient) {
        this.webClient = webClient;
    }

    public String kjørTrivyPå(final String pomFil) {
        return webClient
                .post()
                .uri("/scan")
                .body(BodyInserters.fromValue(pomFil))
                .retrieve()
                .bodyToMono(String.class)
                .retry(1)
                .block();
    }
}
