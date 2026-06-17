package no.nav.teamarbeidsforhold.githubapp.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNvdCveData;
import no.nav.teamarbeidsforhold.githubapp.generert.api.AdminApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
public class AdminController implements AdminApi {
    private final KopierNvdCveData kopierNvdCveData;

    public AdminController(final KopierNvdCveData kopierNvdCveData) {
        this.kopierNvdCveData = kopierNvdCveData;
    }

    @Override
    public ResponseEntity<Void> apiAdminNvdjobbPost() {
        try {
            kopierNvdCveData.kopierNvdCveDataTilDatabase().whenComplete((oppdatert, feil) -> {
                if (feil != null) {
                    log.error("Feil i oppdatering som ble startet manuelt", feil);
                } else if (oppdatert) {
                    log.info("CVE data ble oppdatert");
                } else {
                    log.info("CVE data var allerede oppdatert");
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
