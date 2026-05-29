package no.nav.teamarbeidsforhold.githubapp.components;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.repeat.RepeatSpec;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LederUtpekerServerSideEventHåndterer {

    private final WebClient.Builder builder;
    private final ApplicationEventPublisher publisher;

    private ConfigurableApplicationContext springBootKontekst;

    @Value("${elector.sse.url}")
    private URI uri;

    private WebClient client;

    @PostConstruct
    public void start() {
        client = builder.build();

        client.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(LederUtvelgerRespons.class)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("SSE burde aldri gå tom")))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(rs -> log.warn("Retry SSE for leder utpeking pga feil: {}", rs.failure().toString()))
                )
                .doOnError(e -> {
                    log.error("Feil i SSE for utvelgelse av leder, antar at sidecar er død og avslutter", e);
                    SpringApplication.exit(springBootKontekst, () -> 1);
                })
                .subscribe(
                        response -> publisher.publishEvent(
                                new LederEndretHendelse(response.name())
                        ),
                        error -> log.error("SSE-feil gjør at vi gir opp", error)
                );
    }

    public record LederUtvelgerRespons(
            String name,
            LocalDateTime last_update
    ) {
    }

    public record LederEndretHendelse(String leder) {
    }
}