package no.nav.teamarbeidsforhold.githubapp.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("slack")
public record SlackInnstillinger(boolean enabled, String token,
                                @DefaultValue("#team-arbeidsforhold-sårbarheter") String channel) {
    @Override
    public String toString() {
        return "SlackInnstillinger[enabled=" + enabled + "]";
    }
}
