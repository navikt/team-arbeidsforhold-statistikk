package no.nav.teamarbeidsforhold.githubapp.config;

import io.netty.channel.ChannelOption;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NvdApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@Profile("!local")
public class NvdKonfigurasjon {
    public static final String NVD_ADRESSE = "https://nvd.nist.gov/feeds/json/cve/2.0/";
    public static final String FILNAVN = "nvdcve-2.0-recent";

    @Bean
    @NvdApi
    public WebClient nvdWebClient(final WebClient.Builder builder) {
        return builder
                .baseUrl(NVD_ADRESSE)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().compress(true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                        .responseTimeout(Duration.ofMinutes(2))))
                .codecs(konfigurator -> konfigurator.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
