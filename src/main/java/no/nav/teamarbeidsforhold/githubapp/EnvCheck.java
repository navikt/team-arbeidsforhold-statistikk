package no.nav.teamarbeidsforhold.githubapp;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class EnvCheck {
    private final Environment env;

    @PostConstruct
    public void sjekkUrl() {
        final String url = env.getProperty("spring.datasource.url");

        if (url == null || url.isBlank()) {
            final String systemurl = env.getProperty("PGJDBCURL");
            final String host = env.getProperty("PGHOST");
            final StringBuilder melding = new StringBuilder();
            if (systemurl == null || systemurl.isBlank()) {
                melding.append("ingen pgjdbcurl");
            }
            if (host == null || host.isBlank()) {
                melding.append("ingen pghost");
            }
            throw new IllegalStateException("Url mangler, og " + melding);
        }

        if (!url.startsWith("jdbc:")) {
            throw new IllegalStateException(
                    "Url er feil. Starter med:" + url.substring(0, 4));
        }
    }
}
