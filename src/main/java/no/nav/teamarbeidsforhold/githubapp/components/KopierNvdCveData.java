package no.nav.teamarbeidsforhold.githubapp.components;

import lombok.extern.slf4j.Slf4j;
import no.nav.teamarbeidsforhold.githubapp.config.NvdKonfigurasjon;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvd;
import no.nav.teamarbeidsforhold.githubapp.entity.CveNvdMeta;
import no.nav.teamarbeidsforhold.githubapp.entity.Vulnerability;
import no.nav.teamarbeidsforhold.githubapp.qualifier.NvdApi;
import no.nav.teamarbeidsforhold.githubapp.repository.CveNvdMetaRepository;
import no.nav.teamarbeidsforhold.githubapp.service.VulnerabilityService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
@Profile("!local")
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
    private final CveNvdMetaRepository cveNvdMetaRepository;
    private final VulnerabilityService vulnerabilityService;

    public KopierNvdCveData(@NvdApi final WebClient webClient, final CveNvdMetaRepository cveNvdMetaRepository, final VulnerabilityService vulnerabilityService) {
        this.webClient = webClient;
        this.cveNvdMetaRepository = cveNvdMetaRepository;
        this.vulnerabilityService = vulnerabilityService;
    }

    @Async
    public CompletableFuture<Boolean> kopierNvdCveDataTilDatabase() throws IOException {
        final Optional<CveNvdMeta> nyeMetadata = hentNyeMetadata();
        if (nyeMetadata.isEmpty()) {
            log.info("Data fra NVD har ikke endret seg, gjør ingen oppdatering");
            return CompletableFuture.completedFuture(false);
        }

        final byte[] bytes = webClient.get()
                .uri(NvdKonfigurasjon.FILNAVN + ".json.gz")
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofMinutes(5));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes)))) {
            final JsonNode root = new ObjectMapper().readTree(gzip);
            final JsonNode saarbarheter = root.required("vulnerabilities");
            if (!saarbarheter.isArray()) {
                throw new IOException("NVD-feeden mangler en liste med sårbarheter");
            }
            final List<CveNvd> cveNvds = new ArrayList<>();
            for (final JsonNode v : saarbarheter) {
                final JsonNode cve = v.path("cve");
                final CveNvd cveNvd = new CveNvd();
                final String id = cve.required("id").asString();
                final Vulnerability sårbarhet = new Vulnerability();
                sårbarhet.setId(id);
                cveNvd.setCveId(id);
                cveNvd.setVulnerability(sårbarhet);
                cveNvd.setPublished(sjekkDatoType(cve.required("published").stringValue()));
                cveNvd.setLastModified(sjekkDatoType(cve.required("lastModified").stringValue()));
                final JsonNode cvss = finnCvssData(cve.path("metrics"));
                if (cvss.isMissingNode()) {
                    log.warn("NVD ga ingen CVSS-data for {}", cveNvd.getCveId());
                } else {
                    cveNvd.setCvssVersion(cvss.path("version").asString());
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
        final CveNvdMeta metadata = nyeMetadata.orElseThrow();
        metadata.setTimestamp(OffsetDateTime.now());
        cveNvdMetaRepository.save(metadata);
        return CompletableFuture.completedFuture(true);
    }

    private Optional<CveNvdMeta> hentNyeMetadata() throws IOException {
        final String sisteHash = cveNvdMetaRepository.findTopByOrderByTimestampDesc().map(CveNvdMeta::getSha256).orElse("noe som ikke matcher noen sha256");
        final String serverMeta = webClient.get().uri(NvdKonfigurasjon.FILNAVN + ".meta").retrieve()
                .bodyToMono(String.class).block(Duration.ofMinutes(5));
        final Properties serverproperties = new Properties();
        serverproperties.load(new StringReader(Objects.requireNonNull(serverMeta)));
        final String serverHash = serverproperties.getProperty("sha256");
        if (serverHash == null || !serverHash.matches("[A-Fa-f0-9]{64}")) {
            throw new IOException("NVD-metadata mangler gyldig SHA-256");
        }
        if (sisteHash.equals(serverHash)) {
            return Optional.empty();
        }
        final CveNvdMeta nyMeta = new CveNvdMeta();
        nyMeta.setSha256(serverHash);
        return Optional.of(nyMeta);
    }

    private static JsonNode finnCvssData(final JsonNode metrics) {
        for (final String versjon : List.of("cvssMetricV40", "cvssMetricV31", "cvssMetricV30")) {
            final JsonNode vurderinger = metrics.path(versjon);
            JsonNode vurdering = vurderinger.path(0);
            for (final JsonNode kandidat : vurderinger) {
                if ("Primary".equals(kandidat.path("type").stringValue())) {
                    vurdering = kandidat;
                    break;
                }
            }
            final JsonNode cvssData = vurdering.path("cvssData");
            if (!cvssData.isMissingNode()) {
                return cvssData;
            }
        }
        return MissingNode.getInstance();
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
