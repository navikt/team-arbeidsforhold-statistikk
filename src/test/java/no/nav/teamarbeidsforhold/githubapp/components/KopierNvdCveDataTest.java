package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvdMeta;
import no.nav.teamarbeidsforhold.githubapp.repository.CveNvdMetaRepository;
import no.nav.teamarbeidsforhold.githubapp.service.VulnerabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class KopierNvdCveDataTest {
    private static final String HASH = "a".repeat(64);
    private final CveNvdMetaRepository metadataRepository = mock(CveNvdMetaRepository.class);
    private final VulnerabilityService vulnerabilityService = mock(VulnerabilityService.class);
    private final AtomicReference<CveNvdMeta> lagredeMetadata = new AtomicReference<>();
    private final List<String> foresporsler = new ArrayList<>();
    private String metadata = "sha256=" + HASH;
    private byte[] feed;
    private KopierNvdCveData importør;

    @BeforeEach
    void oppsett() throws IOException {
        feed = gzip(feed("cvssMetricV40", "4.0", ""));
        when(metadataRepository.findTopByOrderByTimestampDesc())
                .thenAnswer(invocation -> Optional.ofNullable(lagredeMetadata.get()));
        when(metadataRepository.save(any(CveNvdMeta.class))).thenAnswer(invocation -> {
            final CveNvdMeta metadata = invocation.getArgument(0);
            lagredeMetadata.set(metadata);
            return metadata;
        });
        final WebClient webClient = WebClient.builder().baseUrl("https://nvd.example.test/")
                .exchangeFunction(foresporsel -> {
                    final String sti = foresporsel.url().getPath();
                    foresporsler.add(sti);
                    if (sti.endsWith(".meta")) {
                        return Mono.just(ClientResponse.create(HttpStatus.OK).body(metadata).build());
                    }
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(feed))).build());
                }).build();
        importør = new KopierNvdCveData(webClient, metadataRepository, vulnerabilityService);
    }

    @ParameterizedTest
    @CsvSource({"cvssMetricV40, 4.0", "cvssMetricV31, 3.1", "cvssMetricV30, 3.0"})
    void lagrer_alvorlighetsgrad_fra_cvss_data_før_metadata(final String felt, final String versjon) throws Exception {
        feed = gzip(feed(felt, versjon, ""));

        assertThat(importør.kopierNvdCveDataTilDatabase().get()).isTrue();

        final ArgumentCaptor<List<CveNvd>> sårbarheter = ArgumentCaptor.captor();
        final InOrder rekkefolge = inOrder(vulnerabilityService, metadataRepository);
        rekkefolge.verify(vulnerabilityService).lagreNvdData(sårbarheter.capture());
        rekkefolge.verify(metadataRepository).save(any(CveNvdMeta.class));
        assertThat(sårbarheter.getValue()).singleElement().satisfies(cve -> {
            assertThat(cve.getCveId()).isEqualTo("CVE-2026-12345");
            assertThat(cve.getBaseSeverity()).isEqualTo("CRITICAL");
            assertThat(cve.getCvssVersion()).isEqualTo(versjon);
            assertThat(cve.getBaseScore()).isEqualByComparingTo("9.8");
            assertThat(cve.getPublished()).isEqualTo(Instant.parse("2026-11-24T05:00:00Z"));
        });
        assertThat(lagredeMetadata.get().getSha256()).isEqualTo(HASH);
        assertThat(lagredeMetadata.get().getTimestamp()).isNotNull();
    }

    @Test
    void velger_primærvurderingen_når_flere_kilder_finnes() throws Exception {
        feed = gzip(feed("cvssMetricV40", "4.0", """
                {"type":"Secondary","cvssData":{"version":"4.0","baseScore":7.0,"baseSeverity":"HIGH"}},
                """));

        importør.kopierNvdCveDataTilDatabase().get();

        verify(vulnerabilityService).lagreNvdData(argThat(sårbarheter ->
                sårbarheter.size() == 1 && "CRITICAL".equals(sårbarheter.getFirst().getBaseSeverity())));
    }

    @Test
    void uendret_feed_laster_ikke_data_på_nytt() throws Exception {
        assertThat(importør.kopierNvdCveDataTilDatabase().get()).isTrue();
        assertThat(importør.kopierNvdCveDataTilDatabase().get()).isFalse();

        assertThat(foresporsler.stream().filter(sti -> sti.endsWith(".json.gz"))).hasSize(1);
        verify(vulnerabilityService, times(1)).lagreNvdData(anyList());
        verify(metadataRepository, times(1)).save(any(CveNvdMeta.class));
    }

    @Test
    void mislykket_lagring_beholder_gammel_hash_slik_at_neste_import_prøver_igjen() throws Exception {
        doThrow(new IllegalStateException("Testfeil")).doNothing().when(vulnerabilityService).lagreNvdData(anyList());

        assertThrows(IllegalStateException.class, () -> importør.kopierNvdCveDataTilDatabase());

        assertThat(lagredeMetadata.get()).isNull();
        verify(metadataRepository, never()).save(any(CveNvdMeta.class));
        assertThat(importør.kopierNvdCveDataTilDatabase().get()).isTrue();
        assertThat(foresporsler.stream().filter(sti -> sti.endsWith(".json.gz"))).hasSize(2);
    }

    @Test
    void ugyldig_gzip_lagrer_ikke_hash() {
        feed = "ikke gzip".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> importør.kopierNvdCveDataTilDatabase());

        verifyNoInteractions(vulnerabilityService);
        verify(metadataRepository, never()).save(any(CveNvdMeta.class));
    }

    @Test
    void ugyldig_feedstruktur_lagrer_ikke_hash() throws IOException {
        feed = gzip("{\"vulnerabilities\":{}}");

        assertThrows(IOException.class, () -> importør.kopierNvdCveDataTilDatabase());

        verifyNoInteractions(vulnerabilityService);
        verify(metadataRepository, never()).save(any(CveNvdMeta.class));
    }

    @Test
    void ugyldige_metadata_avvises_før_nedlasting() {
        metadata = "sha256=ikke-en-hash";

        assertThrows(IOException.class, () -> importør.kopierNvdCveDataTilDatabase());

        assertThat(foresporsler).noneMatch(sti -> sti.endsWith(".json.gz"));
        verifyNoInteractions(vulnerabilityService);
        verify(metadataRepository, never()).save(any(CveNvdMeta.class));
    }

    private static String feed(final String felt, final String versjon, final String andreVurderinger) {
        return """
                {"vulnerabilities":[{"cve":{
                  "id":"CVE-2026-12345",
                  "published":"2026-11-24T05:00:00.000",
                  "lastModified":"2026-11-24T05:30:00.000",
                  "metrics":{"%s":[%s{
                    "type":"Primary",
                    "cvssData":{"version":"%s","baseScore":9.8,"baseSeverity":"CRITICAL"}
                  }]}
                }}]}
                """.formatted(felt, andreVurderinger, versjon);
    }

    private static byte[] gzip(final String tekst) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(tekst.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }
}
