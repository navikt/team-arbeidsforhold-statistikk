package no.nav.teamarbeidsforhold.githubapp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.function.Supplier;

@Configuration
public class GithubConfiguration {
    public static final String ORG_NAVN = "navikt";

    @Bean
    public GitHub github(final AppInstallationAuthorizationProvider autentiserer) throws IOException {
        return new GitHubBuilder().withAuthorizationProvider(autentiserer).build();
    }

    @Bean
    public AppInstallationAuthorizationProvider autentiserer(@Value("${app.githubapp.id}") long appId, final Supplier<PrivateKey> nøkkel) {
        return new AppInstallationAuthorizationProvider(x -> x.getInstallationByOrganization(ORG_NAVN), new JWTTokenProvider(String.valueOf(appId), nøkkel.get()));
    }

    @Bean
    public Supplier<PrivateKey> nøkkel(@Value("${app.private-key}") String formatertNøkkel) {
        return () -> {
            try {
                return JWK.parseFromPEMEncodedObjects(formatertNøkkel).toRSAKey().toPrivateKey();
            } catch (JOSEException e) {
                throw new RuntimeException("Feil i konfigurert nøkkel", e);
            }
        };
    }
}
