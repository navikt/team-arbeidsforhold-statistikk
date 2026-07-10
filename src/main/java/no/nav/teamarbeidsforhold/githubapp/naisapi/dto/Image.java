package no.nav.teamarbeidsforhold.githubapp.naisapi.dto;

import java.util.List;

public record Image(String name, String tag, List<Vulnerability> vulnerabilities) {
}
