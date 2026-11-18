package no.nav.teamarbeidsforhold.githubapp.service;

import no.nav.teamarbeidsforhold.githubapp.components.ManifestParser;
import no.nav.teamarbeidsforhold.githubapp.entity.Deployment;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Ingress;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressGroup;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressOverview;
import no.nav.teamarbeidsforhold.githubapp.repository.DeploymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IngressService {
    private static final Pattern REPOSITORY = Pattern.compile("[A-Za-z0-9-]+/[A-Za-z0-9_.-]+");
    private final DeploymentRepository deploymentRepository;
    private final ManifestParser manifestParser;

    public IngressService(final DeploymentRepository deploymentRepository, final ManifestParser manifestParser) {
        this.deploymentRepository = deploymentRepository;
        this.manifestParser = manifestParser;
    }

    @Transactional(readOnly = true)
    public IngressOverview oversikt() {
        final Map<IngressGroup.ExposureEnum, IngressGroup> grupper = new EnumMap<>(IngressGroup.ExposureEnum.class);
        leggTilGruppe(grupper, IngressGroup.ExposureEnum.INTERNET, "Internett",
                "Antatt tilgjengelig fra internett: nav.no og ekstern.dev.nav.no.");
        leggTilGruppe(grupper, IngressGroup.ExposureEnum.UNKNOWN, "Uavklart tilgang",
                "Ukjent domene eller ugyldig URL. Vurder manuelt før disse prioriteres lavere.");
        leggTilGruppe(grupper, IngressGroup.ExposureEnum.EMPLOYEES, "Ansatte",
                "ansatt.nav.no og ansatt.dev.nav.no: autentiserte mennesker på godkjente enheter.");
        leggTilGruppe(grupper, IngressGroup.ExposureEnum.INTERNAL, "Nav-internt",
                "Interne nettverk, inkludert naisdevice. Tilleggskrav kan gjelde for on-prem.");
        leggTilGruppe(grupper, IngressGroup.ExposureEnum.GCP, "GCP til on-prem",
                "prod-fss-pub.nais.io og dev-fss-pub.nais.io: tilgjengelig fra GCP.");

        int utenManifest = 0;
        for (final Deployment deployment : deploymentRepository.findAll()) {
            if (deployment.getManifest() == null || deployment.getManifest().isBlank()) {
                utenManifest++;
                continue;
            }
            final var manifest = manifestParser.parse(deployment.getManifest());
            if (manifest.spec() == null || manifest.spec().ingresses() == null) {
                continue;
            }
            for (final String url : manifest.spec().ingresses().stream().distinct().toList()) {
                final var id = deployment.getId();
                final var ingress = new Ingress(url, id.getWorkloadName(), id.getEnvironment(), id.getSuffix())
                        .repository(repository(deployment.getLastCommit()));
                if (deployment.getLastSeen() != null) {
                    ingress.setLastSeen(deployment.getLastSeen().atOffset(ZoneOffset.UTC));
                }
                grupper.get(eksponering(url)).addIngressesItem(ingress);
            }
        }
        final Comparator<Ingress> sortering = Comparator.comparing(Ingress::getUrl)
                .thenComparing(Ingress::getWorkload)
                .thenComparing(Ingress::getEnvironment)
                .thenComparing(Ingress::getSuffix);
        final List<IngressGroup> resultat = new ArrayList<>();
        for (final IngressGroup gruppe : grupper.values()) {
            if (!gruppe.getIngresses().isEmpty()) {
                gruppe.getIngresses().sort(sortering);
                resultat.add(gruppe);
            }
        }
        return new IngressOverview(resultat, utenManifest);
    }

    private static void leggTilGruppe(final Map<IngressGroup.ExposureEnum, IngressGroup> grupper,
                                     final IngressGroup.ExposureEnum eksponering, final String navn, final String beskrivelse) {
        grupper.put(eksponering, new IngressGroup(eksponering, navn, beskrivelse, new ArrayList<>()));
    }

    private static IngressGroup.ExposureEnum eksponering(final String url) {
        if (url == null) {
            throw new IllegalArgumentException("Manifestet inneholder en ingress uten URL");
        }
        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return IngressGroup.ExposureEnum.UNKNOWN;
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null) {
            return IngressGroup.ExposureEnum.UNKNOWN;
        }
        final String host = uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("\\.$", "");
        if (domene(host, "ansatt.nav.no") || domene(host, "ansatt.dev.nav.no")) {
            return IngressGroup.ExposureEnum.EMPLOYEES;
        }
        if (domene(host, "intern.nav.no") || domene(host, "intern.dev.nav.no") || domene(host, "nais.adeo.no")) {
            return IngressGroup.ExposureEnum.INTERNAL;
        }
        if (domene(host, "prod-fss-pub.nais.io") || domene(host, "dev-fss-pub.nais.io")) {
            return IngressGroup.ExposureEnum.GCP;
        }
        if (domene(host, "nav.no")) {
            return IngressGroup.ExposureEnum.INTERNET;
        }
        return IngressGroup.ExposureEnum.UNKNOWN;
    }

    private static boolean domene(final String host, final String domene) {
        return host.equals(domene) || host.endsWith("." + domene);
    }

    private static String repository(final String lastCommit) {
        if (lastCommit == null || !lastCommit.contains("@")) {
            return null;
        }
        String repository = lastCommit.substring(0, lastCommit.lastIndexOf('@'));
        if (repository.startsWith("https://github.com/")) {
            repository = repository.substring("https://github.com/".length());
        }
        if (repository.endsWith(".git")) {
            repository = repository.substring(0, repository.length() - ".git".length());
        }
        return REPOSITORY.matcher(repository).matches() ? repository : null;
    }
}
