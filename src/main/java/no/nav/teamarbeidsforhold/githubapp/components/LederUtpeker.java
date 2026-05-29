package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class LederUtpeker {

    private final String hostname;
    private AtomicBoolean erLeder = new AtomicBoolean(false);

    public LederUtpeker() throws Exception {
        this.hostname = InetAddress.getLocalHost().getHostName();
    }

    @EventListener
    public void onLeaderChanged(LederUtpekerServerSideEventHåndterer.LederEndretHendelse event) {
        boolean ny = hostname.equals(event.leder());
        if (erLeder.compareAndSet(!ny, ny)) {
            log.info("Lederstatus endret: {}", ny);
        }
    }

    public boolean erLeder() {
        return erLeder.get();
    }
}
