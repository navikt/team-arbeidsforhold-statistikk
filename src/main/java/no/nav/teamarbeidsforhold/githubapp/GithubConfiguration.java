package no.nav.teamarbeidsforhold.githubapp;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class GithubConfiguration {
    public static final String ORG_NAVN = "navikt";

    @Bean
    public GitHub github(final AppInstallationAuthorizationProvider autentiserer) throws IOException {
        return new GitHubBuilder().withAuthorizationProvider(autentiserer).build();
    }

    @Bean
    public AppInstallationAuthorizationProvider autentiserer(@Value("${client-id}") String clientId, final PrivateKey nøkkel) {
        return new AppInstallationAuthorizationProvider(x -> x.getInstallationByOrganization(ORG_NAVN), new JWTTokenProvider(clientId, nøkkel));
    }

    @Bean
    public PrivateKey nøkkel(@Value("${private-key}") String formatertNøkkel) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final String råNøkkel = formatertNøkkel
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----s", "")
                .replaceAll("\\s+", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(råNøkkel)));
    }
}
