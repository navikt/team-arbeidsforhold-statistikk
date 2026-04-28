package no.nav.teamarbeidsforhold.githubapp.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class StoppSpringBootHvisTrådFeilerThreadFactory implements ThreadFactory, HealthIndicator {

    private final AtomicReference<Thread> trå = new AtomicReference<>();

    @Override
    public Thread newThread(final Runnable r) {
        if (!trå.compareAndSet(null, new Thread(r, "vital-arbeider"))) {
            throw new IllegalStateException("ThreadFactory er kun beregnet for enkelt-trå Executor");
        }
        trå.get().setUncaughtExceptionHandler((_, exception) -> {
            log.error("Vital arbeider feilet, ber spring boot om å stenge ned", exception);
            StoppSpringBoot.feil(exception);
        });
        return trå.get();
    }

    @Override
    public Health health() {
        if (trå.get() == null) {
            return Health.unknown().build();
        }
        if (trå.get().isAlive()) {
            return Health.up().build();
        }
        return Health.down().build();
    }
}
