package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.config.NvdKonfigurasjon;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNdvMeta;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NvdApi;
import no.nav.teamarbeidsforhold.githubapp.repository.CveNdvMetaRepository;
import no.nav.teamarbeidsforhold.githubapp.repository.CveNvdRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class KopierNvdCveData {
    public static final DateTimeFormatter FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .optionalStart()
                    .appendLiteral('T')
                    .optionalEnd()
                    .optionalStart()
                    .appendLiteral(' ')
                    .optionalEnd()
                    .optionalStart()
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .optionalEnd()
                    .optionalStart()
                    .appendOffsetId()
                    .optionalEnd()
                    .optionalStart()
                    .appendLiteral('Z')
                    .optionalEnd()
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .toFormatter();

    private final WebClient webClient;
    private final CveNvdRepository cveNvdRepository;
    private final CveNdvMetaRepository cveNdvMetaRepository;

    public KopierNvdCveData(@NvdApi final WebClient webClient, final CveNvdRepository cveNvdRepository, final CveNdvMetaRepository cveNdvMetaRepository) {
        this.webClient = webClient;
        this.cveNvdRepository = cveNvdRepository;
        this.cveNdvMetaRepository = cveNdvMetaRepository;
    }

    @Async
    public CompletableFuture<Boolean> kopierNvdCveDataTilDatabase() throws IOException {
        final String sisteMeta = cveNdvMetaRepository.findTopByOrderByTimestampDesc().map(CveNdvMeta::getSha256).orElse("noe som ikke matcher noen sha256");
        final String serverMeta = webClient.get().uri(NvdKonfigurasjon.FILNAVN + ".meta").retrieve().bodyToMono(String.class).block();
        if (sisteMeta.equals(serverMeta)) {
            log.info("Data fra NVD har ikke endret seg, gjør ingen oppdatering");
            return CompletableFuture.completedFuture(false);
        }

        final byte[] bytes = webClient.get()
                .uri(NvdKonfigurasjon.FILNAVN + ".json.gz")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            final JsonNode root = new ObjectMapper().readTree(gzip);
            final List<CveNvd> cveNvds = new ArrayList<>();
            for (JsonNode v : root.path("vulnerabilities")) {
                final JsonNode cve = v.path("cve");
                final CveNvd cveNvd = new CveNvd();
                cveNvd.setCveId(cve.required("id").asString());
                cveNvd.setPublished(sjekkDatoType(cve.required("published").stringValue()));
                cveNvd.setLastModified(sjekkDatoType(cve.required("lastModified").stringValue()));
                final JsonNode cvss = cve.path("metrics")
                        .path("cvssMetricV40")
                        .path(0);
                if (cvss == null || cvss.isNull() || cvss.isMissingNode()) {
                    log.warn("Parsing of cvss 4.0 failed, but continuing, treating it as optional field");
                } else {
                    cveNvd.setCvssVersion(cvss.path("cvssVersion").asString());
                    cveNvd.setVectorString(cvss.path("vectorString").asString());
                    cveNvd.setBaseScore(sjekkTall(cvss));


                    cveNvd.setBaseSeverity(cvss.path("baseSeverity").asString());
                    cveNvd.setAttackVector(cvss.path("attackVector").asString());
                    cveNvd.setAttackComplexity(cvss.path("attackComplexity").asString());
                    cveNvd.setAttackRequirements(cvss.path("attackRequirements").asString());
                    cveNvd.setPrivilegesRequired(cvss.path("privilegesRequired").asString());
                    cveNvd.setUserInteraction(cvss.path("userInteraction").asString());

                    cveNvd.setVc(cvss.path("vulnerableConfidentialityImpact").asString());
                    cveNvd.setVi(cvss.path("vulnerableIntegrityImpact").asString());
                    cveNvd.setVa(cvss.path("vulnerableAvailabilityImpact").asString());

                    cveNvd.setSc(cvss.path("subsequentConfidentialityImpact").asString());
                    cveNvd.setSi(cvss.path("subsequentIntegrityImpact").asString());
                    cveNvd.setSa(cvss.path("subsequentAvailabilityImpact").asString());

                    cveNvd.setExploitMaturity(cvss.path("exploitMaturity").asString());

                    cveNvd.setModifiedAttackVector(cvss.path("modifiedAttackVector").asString());
                    cveNvd.setModifiedAttackComplexity(cvss.path("modifiedAttackComplexity").asString());
                    cveNvd.setModifiedAttackRequirements(cvss.path("modifiedAttackRequirements").asString());
                    cveNvd.setModifiedPrivilegesRequired(cvss.path("modifiedPrivilegesRequired").asString());
                    cveNvd.setModifiedUserInteraction(cvss.path("modifiedUserInteraction").asString());

                    cveNvd.setModifiedVc(cvss.path("modifiedVulnerableConfidentialityImpact").asString());
                    cveNvd.setModifiedVi(cvss.path("modifiedVulnerableIntegrityImpact").asString());
                    cveNvd.setModifiedVa(cvss.path("modifiedVulnerableAvailabilityImpact").asString());
                    cveNvds.add(cveNvd);
                }
            }
            cveNvdRepository.saveAll(cveNvds);
        }
        return CompletableFuture.completedFuture(true);
    }

    private static BigDecimal sjekkTall(final JsonNode cvss) {
        try {
            final float primitiv = cvss.path("baseScore").asFloat();
            return new BigDecimal(primitiv);
        } catch (final NumberFormatException e) {
            try {
                final String tekst = cvss.path("baseScore").asString();
                if (tekst != null && !tekst.isBlank()) {
                    log.warn("baseScore var ikke tall, men var \"{}\"", tekst);
                    return new BigDecimal(tekst);
                } else {
                    return null;
                }
            } catch (final NumberFormatException e2) {
                return null;
            }
        }
    }

    private Instant sjekkDatoType(final String tekst) {
        final TemporalAccessor tidsData = FORMATTER.parse(tekst);
        if (tidsData.isSupported(ChronoField.INSTANT_SECONDS)) {
            return Instant.from(tidsData);
        } else {
            return LocalDateTime.from(tidsData).atOffset(ZoneOffset.UTC).toInstant();
        }
    }
}
