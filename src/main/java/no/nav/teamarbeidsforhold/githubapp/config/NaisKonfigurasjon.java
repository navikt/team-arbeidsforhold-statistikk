package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.qualifier.NaisApi;
import no.nav.teamarbeidsforhold.githubapp.qualifier.TrivyApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Configuration
public class NaisKonfigurasjon {
    private static final String NAIS_API_ADRESSE = "https://console.nav.cloud.nais.io/graphql";

    @Bean
    @NaisApi
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

    @Bean
    @NaisApi
    public WebClient naisWebClient(final WebClient.Builder builder, @NaisApi final Supplier<String> token) {
        return builder
                .baseUrl(NAIS_API_ADRESSE)
                .filter((forespørsel, nesteFilter) ->
                        nesteFilter.exchange(ClientRequest.from(forespørsel).headers(headers -> headers.setBearerAuth(token.get())).build()))
                .build();
    }


    @Bean
    @NaisApi
    HttpGraphQlClient externalApiClient(@NaisApi WebClient webClient) {
        return HttpGraphQlClient.create(webClient);
    }
}
