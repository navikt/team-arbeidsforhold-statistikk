package no.nav.teamarbeidsforhold.githubapp.naisapi.dto;

import java.util.List;

public record PaginatedVulnerabilities(List<Vulnerability> nodes) {
}
