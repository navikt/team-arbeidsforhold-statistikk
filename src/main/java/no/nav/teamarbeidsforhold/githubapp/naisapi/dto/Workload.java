package no.nav.teamarbeidsforhold.githubapp.naisapi.dto;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Workload(String name, Manifest manifest, PaginatedDeployments deployments, TeamEnvironment teamEnvironment, Image image, MiljøSpesifisertNavn miljøSpesifisertNavn) {
    private static final Pattern MILJØ_SPESIFISERT_NAVN = Pattern.compile("([a-zA-Z\\-0-9]+-)(q\\d)(-[a-zA-Z\\-0-9])?");

    public Workload {
        Objects.requireNonNull(name);
        if (miljøSpesifisertNavn == null) {
            final Matcher matcher = MILJØ_SPESIFISERT_NAVN.matcher(name);
            if (matcher.matches()) {
                miljøSpesifisertNavn = new MiljøSpesifisertNavn(matcher.group(1) + (matcher.group(3) == null ? "" : matcher.group(3)), matcher.group(2));
            } else {
                miljøSpesifisertNavn = new MiljøSpesifisertNavn(name, null);
            }
        }
    }
}
