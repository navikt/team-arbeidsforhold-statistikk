package no.nav.teamarbeidsforhold.githubapp.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.util.http.SlackHttpClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.teamarbeidsforhold.githubapp.components.KritiskeSårbarheterAnalyse;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNaisApiData;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNvdCveData;
import no.nav.teamarbeidsforhold.githubapp.components.LederUtpeker;
import no.nav.teamarbeidsforhold.githubapp.components.SlackSender;
import no.nav.teamarbeidsforhold.githubapp.components.TidsstyrteHandlinger;
import no.nav.teamarbeidsforhold.githubapp.properties.SlackInnstillinger;
import no.nav.teamarbeidsforhold.githubapp.repository.CriticalVulnerabilityNoticeRepository;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import java.io.IOException;
import java.net.Proxy;
import java.time.Clock;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SlackKonfigurasjonTest {
    private final ApplicationContextRunner kontekst = new ApplicationContextRunner()
            .withUserConfiguration(Egenskaper.class, SlackKonfigurasjon.class, TidsstyringKonfigurasjon.class,
                    SlackSender.class, TidsstyrteHandlinger.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withBean(CriticalVulnerabilityNoticeRepository.class, () -> mock(CriticalVulnerabilityNoticeRepository.class))
            .withBean(KritiskeSårbarheterAnalyse.class, () -> mock(KritiskeSårbarheterAnalyse.class))
            .withBean(LederUtpeker.class, () -> mock(LederUtpeker.class))
            .withBean(KopierNvdCveData.class, () -> mock(KopierNvdCveData.class))
            .withBean(KopierNaisApiData.class, () -> mock(KopierNaisApiData.class));

    @Test
    void utenTokenStopperOppstartUtenÅLekkeKonfigurasjon() {
        kontekst.run(kontekst -> {
            assertThat(kontekst).hasFailed();
            assertThat(kontekst.getStartupFailure()).hasRootCauseMessage(
                    "slack.token må være satt og bare inneholde synlige ASCII-tegn");
        });
    }

    @Test
    void binderStandardkanalOgOppretterPlanlegging() {
        kontekst.withPropertyValues("slack.token=dummy-token").run(kontekst -> {
            assertThat(kontekst).hasNotFailed().hasSingleBean(SlackSender.class)
                    .hasSingleBean(TidsstyrteHandlinger.class);
            assertThat(kontekst).hasBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
            assertThat(kontekst.getBean(SlackInnstillinger.class).channel())
                    .isEqualTo("#team-arbeidsforhold-sårbarheter");
            assertThat(kontekst.getBean(SlackInnstillinger.class).toString()).doesNotContain("dummy-token");
        });
    }

    @Test
    void deaktivertOppdateringFjernerPlanleggingSelvOmSlackErAktivert() {
        kontekst.withPropertyValues("slack.token=dummy-token", "oppdatering.enabled=false")
                .run(kontekst -> {
                    assertThat(kontekst).hasNotFailed().hasSingleBean(Slack.class)
                            .hasSingleBean(SlackSender.class).doesNotHaveBean(TidsstyrteHandlinger.class);
                    assertThat(kontekst)
                            .doesNotHaveBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
                });
    }

    @Test
    void lokalprofilOppretterAldriSlackSelvMedTokenOgAktivering() {
        kontekst.withPropertyValues("slack.token=dummy-token")
                .withInitializer(kontekst -> kontekst.getEnvironment().setActiveProfiles("local"))
                .run(kontekst -> assertThat(kontekst).hasNotFailed().doesNotHaveBean(Slack.class)
                        .doesNotHaveBean(SlackSender.class).doesNotHaveBean(TidsstyrteHandlinger.class)
                        .doesNotHaveBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "https://example.test", "<!channel>", "#kanal\nmed-linjeskift"})
    void avviserUgyldigKanal(final String kanal) {
        kontekst.withPropertyValues("slack.token=dummy-token", "slack.channel=" + kanal)
                .run(kontekst -> assertThat(kontekst).hasFailed());
    }

    @Test
    void http503MedRetryAfterNullSkalIkkeGiSkjultGjentakelse() throws Exception {
        final WireMockServer stub = new WireMockServer(options().dynamicPort());
        stub.start();
        try (final Slack produksjonsklient = new SlackKonfigurasjon().slack(
                new SlackInnstillinger("dummy-token", "#testkanal"))) {
            stub.stubFor(post(urlEqualTo("/api/chat.postMessage"))
                    .willReturn(aResponse().withStatus(503).withHeader("Retry-After", "0").withBody("{}")));
            try (final Slack lokalKlient = lokalKlient(produksjonsklient, stub)) {
                assertThrows(SlackApiException.class, () -> lokalKlient.methods("dummy-token")
                        .chatPostMessage(ChatPostMessageRequest.builder().channel("#testkanal").text("Test").build()));
            }
            stub.verify(1, postRequestedFor(urlEqualTo("/api/chat.postMessage")));
            stub.verify(0, anyRequestedFor(urlEqualTo("/api/auth.test")));
        } finally {
            stub.stop();
        }
    }

    @Test
    void bruttForbindelseSkalGiEttHttpKall() throws Exception {
        final WireMockServer stub = new WireMockServer(options().dynamicPort());
        stub.start();
        try (final Slack produksjonsklient = new SlackKonfigurasjon().slack(
                new SlackInnstillinger("dummy-token", "#testkanal"))) {
            stub.stubFor(post(urlEqualTo("/api/chat.postMessage"))
                    .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
            try (final Slack lokalKlient = lokalKlient(produksjonsklient, stub)) {
                assertThrows(IOException.class, () -> lokalKlient.methods("dummy-token")
                        .chatPostMessage(ChatPostMessageRequest.builder().channel("#testkanal").text("Test").build()));
            }
            stub.verify(1, postRequestedFor(urlEqualTo("/api/chat.postMessage")));
        } finally {
            stub.stop();
        }
    }

    private static Slack lokalKlient(final Slack produksjonsklient, final WireMockServer stub) {
        final SlackConfig konfigurasjon = new SlackConfig();
        konfigurasjon.setStatsEnabled(false);
        konfigurasjon.setMethodsEndpointUrlPrefix(stub.baseUrl() + "/api/");
        final OkHttpClient httpKlient = produksjonsklient.getHttpClient().getOkHttpClient().newBuilder()
                .proxy(Proxy.NO_PROXY).connectionSpecs(List.of(ConnectionSpec.CLEARTEXT)).build();
        return Slack.getInstance(konfigurasjon, new SlackHttpClient(httpKlient));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SlackInnstillinger.class)
    static class Egenskaper {
    }
}
