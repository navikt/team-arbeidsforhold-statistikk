package no.nav.teamarbeidsforhold.githubapp.dto;

import java.util.List;

public record Workload(String name, Manifest manifest, List<Deployment> deployments, TeamEnvironment teamEnvironment, Image image) {
}
