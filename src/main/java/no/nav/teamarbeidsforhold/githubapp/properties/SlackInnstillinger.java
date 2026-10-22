package no.nav.teamarbeidsforhold.githubapp.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("slack")
public record SlackInnstillinger(String token) {
}
