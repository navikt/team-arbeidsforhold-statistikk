package no.nav.teamarbeidsforhold.githubapp.config;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.methods.MethodsClient;
import com.slack.api.util.http.SlackHttpClient;
import no.nav.teamarbeidsforhold.githubapp.properties.SlackInnstillinger;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
@Profile("!local")
@ConditionalOnProperty(prefix = "slack", name = "enabled", havingValue = "true")
public class SlackKonfigurasjon {
    private static final Pattern KANAL = Pattern.compile("(?:[CG][A-Z0-9]+|#[\\p{L}0-9_-]+)");

    @Bean(destroyMethod = "close")
    Slack slack(final SlackInnstillinger innstillinger) {
        if (innstillinger.token() == null || innstillinger.token().isBlank()
                || innstillinger.token().chars().anyMatch(tegn -> tegn <= 32 || tegn >= 127)) {
            throw new IllegalStateException(
                    "slack.token må være satt og bare inneholde synlige ASCII-tegn når Slack er aktivert");
        }
        if (innstillinger.channel() == null || !KANAL.matcher(innstillinger.channel()).matches()) {
            throw new IllegalStateException("slack.channel må være en kanal-ID eller et kanalnavn som starter med #");
        }
        final SlackConfig konfigurasjon = new SlackConfig();
        konfigurasjon.setStatsEnabled(false);
        konfigurasjon.setPrettyResponseLoggingEnabled(false);
        konfigurasjon.setHttpClientResponseHandlers(List.of());
        final OkHttpClient httpKlient = SlackHttpClient.buildOkHttpClient(konfigurasjon).newBuilder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectionSpecs(List.of(ConnectionSpec.MODERN_TLS))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .writeTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(30))
                .addInterceptor(kjede -> kjede.proceed(kjede.request().newBuilder()
                        .method(kjede.request().method(), utenGjentakelse(kjede.request().body())).build()))
                .build();
        return Slack.getInstance(konfigurasjon, new SlackHttpClient(httpKlient));
    }

    @Bean
    MethodsClient slackMethods(final Slack slack, final SlackInnstillinger innstillinger) {
        return slack.methods(innstillinger.token());
    }

    private static RequestBody utenGjentakelse(final RequestBody innhold) {
        if (innhold == null) {
            return null;
        }
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return innhold.contentType();
            }

            @Override
            public long contentLength() throws IOException {
                return innhold.contentLength();
            }

            @Override
            public void writeTo(final BufferedSink buffer) throws IOException {
                innhold.writeTo(buffer);
            }

            @Override
            public boolean isOneShot() {
                return true;
            }
        };
    }
}
