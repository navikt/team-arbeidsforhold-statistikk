package no.nav.teamarbeidsforhold.githubapp.components;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.worker.GithubOppdatering;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class GithubOppdateringsKø {
    private final BlockingQueue<GithubOppdatering> queue = new LinkedBlockingQueue<>(100);
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;
    private final GitHub api;

    public GithubOppdateringsKø(@Qualifier("github") final Executor executor, final TransactionTemplate transactionTemplate, final GitHub api) {
        this.executor = executor;
        this.transactionTemplate = transactionTemplate;
        this.api = api;
    }

    @PostConstruct
    public void startTrå() {
        executor.execute(this::oppdateringsLoop);
    }

    public boolean oppdater(final GithubOppdatering oppdatering) {
        return queue.offer(oppdatering);
    }

    private void oppdateringsLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final GithubOppdatering oppdatering = queue.take();
                transactionTemplate.<Void>execute(_ -> {
                    try {
                        oppdatering.kjør(api);
                    } catch (final Exception e) {
                        log.info("Oppdatering feilet", e);
                    }
                    return null;
                });
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}