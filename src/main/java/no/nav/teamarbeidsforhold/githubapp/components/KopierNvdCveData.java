package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.config.NvdKonfigurasjon;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNdvMeta;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NvdApi;
import no.nav.teamarbeidsforhold.githubapp.repository.CveNdvMetaRepository;
import no.nav.teamarbeidsforhold.githubapp.service.VulnerabilityService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
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
    private final CveNdvMetaRepository cveNdvMetaRepository;
    private final VulnerabilityService vulnerabilityService;

    public KopierNvdCveData(@NvdApi final WebClient webClient, final CveNdvMetaRepository cveNdvMetaRepository, final VulnerabilityService vulnerabilityService) {
        this.webClient = webClient;
        this.cveNdvMetaRepository = cveNdvMetaRepository;
        this.vulnerabilityService = vulnerabilityService;
    }

    @Async
    public CompletableFuture<Boolean> kopierNvdCveDataTilDatabase() throws IOException {
        if (!indikererMetadataNyeDataOgHvisSåLagreMetadata()) {
            log.info("Data fra NVD har ikke endret seg, gjør ingen oppdatering");
            return CompletableFuture.completedFuture(true);
        }

        final byte[] bytes = webClient.get()
                .uri(NvdKonfigurasjon.FILNAVN + ".json.gz")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes)))) {
            final JsonNode root = new ObjectMapper().readTree(gzip);
            final List<CveNvd> cveNvds = new ArrayList<>();
            for (JsonNode v : root.path("vulnerabilities")) {
                final JsonNode cve = v.path("cve");
                final CveNvd cveNvd = new CveNvd();
                cveNvd.setCveId(cve.required("id").asString());
                cveNvd.setPublished(sjekkDatoType(cve.required("published").stringValue()));
                cveNvd.setLastModified(sjekkDatoType(cve.required("lastModified").stringValue()));
                final JsonNode cvsser = cve.path("metrics").path("cvssMetricV40");
                final JsonNode cvss = cvsser.path(0);
                if (cvsser.size() > 2) {
                    log.error("Kan ikke håndtere to forskjellige cvss-verdier, vi må skrive kode for å velge/prioritere");
                } else if (cvss.isMissingNode()) {
                    log.warn("Server ga oss ikke cvss for {}", cveNvd.getCveId());
                } else {
                    cveNvd.setCvssVersion(cvss.path("cvssVersion").asString());
                    cveNvd.setVectorString(cvss.path("vectorString").asString());
                    cveNvd.setBaseScore(cvss.path("baseScore").asDecimal(null));

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
                }
                cveNvds.add(cveNvd);
            }
            vulnerabilityService.lagreNvdData(cveNvds);
        }
        return CompletableFuture.completedFuture(true);
    }

    private boolean indikererMetadataNyeDataOgHvisSåLagreMetadata() throws IOException {
        final String sisteHash = cveNdvMetaRepository.findTopByOrderByTimestampDesc().map(CveNdvMeta::getSha256).orElse("noe som ikke matcher noen sha256");
        final String serverMeta = webClient.get().uri(NvdKonfigurasjon.FILNAVN + ".meta").retrieve().bodyToMono(String.class).block();
        final Properties serverproperties = new Properties();
        serverproperties.load(new StringReader(Objects.requireNonNull(serverMeta)));
        final String serverHash = serverproperties.getProperty("sha256");
        if (sisteHash.equals(serverHash)) {
            return false;
        }
        final CveNdvMeta nyMeta = new CveNdvMeta();
        nyMeta.setSha256(serverHash);
        nyMeta.setTimestamp(OffsetDateTime.now());
        cveNdvMetaRepository.save(nyMeta);
        return true;
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
