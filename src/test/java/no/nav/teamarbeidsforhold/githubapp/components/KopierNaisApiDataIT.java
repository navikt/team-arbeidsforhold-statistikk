package no.nav.teamarbeidsforhold.githubapp.components;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import no.nav.teamarbeidsforhold.githubapp.TestKeysConfig;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NaisApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestKeysConfig.class)
public final class KopierNaisApiDataIT {
    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "nais.api.url",
                () -> wireMockServer.baseUrl() + "/graphql"
        );
    }

    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    @NaisApi
    Supplier<String> naisToken;
    @Autowired
    KopierNaisApiData kopierNaisApiData;

    @Test
    void testGraphqlKall() throws InterruptedException, ExecutionException {
        when(naisToken.get()).thenReturn("test-token");
        final String manifest = """
                apiVersion: nais.io/v1alpha1
                kind: Application
                metadata:
                  labels:
                    team: it-team
                  name: it-app
                  namespace: it-team
                """;
        final String jsonReply = """
                {
                  "data": {
                    "team": {
                      "workloads": {
                        "nodes": [
                          {
                            "name": "it-app",
                            "manifest": {
                              "content": %s
                            },
                            "deployments": {
                              "nodes": []
                            },
                            "image": {
                              "name": "it-app-image",
                              "tag": "tag-it",
                              "vulnerabilities": {
                                "nodes": []
                              }
                            },
                            "teamEnvironment": {
                              "environment": {
                                "name": "it-test-environment"
                              }
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """.formatted(objectMapper.writeValueAsString(manifest));
        wireMockServer.stubFor(post("/graphql")
                .withRequestBody(matchingJsonPath("$.query"))
                .withRequestBody(containing("query WorkloadsMedCriticalCve")).willReturn(okJson(jsonReply)));
        kopierNaisApiData.kopierNaisApiDataTilDatabase().get();
        wireMockServer.verify(postRequestedFor(urlEqualTo("/graphql")));
    }
}
