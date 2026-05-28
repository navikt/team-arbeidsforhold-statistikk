package no.nav.teamarbeidsforhold.githubapp.components;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public final class TrivyKjører {
    private final WebClient webClient;

    public TrivyKjører(final WebClient webClient) {
        this.webClient = webClient;
    }

    public String kjørTrivyPå(final String pomFil) {
        return webClient
                .post()
                .body(BodyInserters.fromValue(pomFil))
                .retrieve()
                .bodyToMono(String.class)
                .retry(1)
                .block();
    }
}
