package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.qualifier.NaisApi;
import no.nav.teamarbeidsforhold.githubapp.qualifier.TrivyApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.support.DocumentSource;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Configuration
public class NaisKonfigurasjon {
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
    public WebClient naisWebClient(final WebClient.Builder builder, @NaisApi final Supplier<String> token, @Value("${nais.api.url}") final String naisApiUrl) {
        return builder
                .baseUrl(naisApiUrl)
                .filter((forespørsel, nesteFilter) ->
                        nesteFilter.exchange(ClientRequest.from(forespørsel).headers(headers -> headers.setBearerAuth(token.get())).build()))
                .build();
    }


    @Bean
    @NaisApi
    HttpGraphQlClient apiKlient(@NaisApi WebClient webClient) {
        return HttpGraphQlClient.builder(webClient).documentSource(name -> Mono.fromCallable(() ->
                new ClassPathResource("graphql/queries/" + name + ".graphql").getContentAsString(StandardCharsets.UTF_8))).build();
    }
}
