package no.nav.teamarbeidsforhold.githubapp.controller;

import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.lockservice.LockServiceFactory;
import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNvdCveData;
import no.nav.teamarbeidsforhold.githubapp.generert.api.AdminApi;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
@Slf4j
public class AdminController implements AdminApi {
    private final KopierNvdCveData kopierNvdCveData;
    private final SpringLiquibase liquibase;
    private final DataSource dataSource;

    public AdminController(final KopierNvdCveData kopierNvdCveData, final SpringLiquibase liquibase, final DataSource dataSource) {
        this.kopierNvdCveData = kopierNvdCveData;
        this.liquibase = liquibase;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResponseEntity<Void> apiAdminLiquibaseUnlockPost() {
        try (Connection connection = dataSource.getConnection()) {
            liquibase.afterPropertiesSet();
            LockServiceFactory.getInstance().getLockService(DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))).forceReleaseLock();
        } catch (final LiquibaseException | SQLException e) {
            log.error("Feil oppstod under forsøk på å låse opp liquibase for ny deploy", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
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
        return ResponseEntity.accepted().build();
    }
}
