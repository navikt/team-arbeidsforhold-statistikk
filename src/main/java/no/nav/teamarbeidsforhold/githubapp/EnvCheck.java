package no.nav.teamarbeidsforhold.githubapp;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

public class EnvCheck implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final String url = environment.getProperty("spring.datasource.url");

        if (url == null || url.isBlank()) {
            final String systemurl = environment.getProperty("PGJDBCURL");
            final String host = environment.getProperty("PGHOST");
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
