package no.nav.teamarbeidsforhold.githubapp.dto;

import java.time.LocalDateTime;

public record Deployment(LocalDateTime createdAt, String commitSha, String deployerUsername, String repository) {
}
