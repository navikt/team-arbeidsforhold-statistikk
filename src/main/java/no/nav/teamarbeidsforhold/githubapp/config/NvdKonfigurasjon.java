package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.qualifier.NvdApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class NvdKonfigurasjon {
    public static final String NVD_ADRESSE = "https://nvd.nist.gov/feeds/json/cve/2.0/";
    public static final String FILNAVN = "nvdcve-2.0-recent";

    @Bean
    @NvdApi
    public WebClient nvdWebClient(final WebClient.Builder builder) {
        return builder.baseUrl(NVD_ADRESSE).clientConnector(new ReactorClientHttpConnector(HttpClient.create().compress(true))).build();
    }
}
