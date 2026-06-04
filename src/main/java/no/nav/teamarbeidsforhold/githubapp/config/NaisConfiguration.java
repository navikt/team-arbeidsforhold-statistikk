package no.nav.teamarbeidsforhold.githubapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Configuration
public class NaisConfiguration {
    @Bean
    public Supplier<String> naisToken(@Value("${NAIS_SERVICE_ACCOUNT_TOKEN_PATH:?NAIS_SERVICE_ACCOUNT_TOKEN_PATH manglet}") final String naisApiTokenSti) {
        final Path sti = Path.of(naisApiTokenSti);
        return () -> {
            try {
                return Files.readString(sti);
            } catch (IOException e) {
                throw new RuntimeException("Feil i lesing av token (vår antagelse om at Nais bytter ut filen atomisk ved rullering er kanskje feil)", e);
            }
        };
    }
}
