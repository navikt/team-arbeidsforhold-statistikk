package no.nav.teamarbeidsforhold.githubapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
@ConditionalOnProperty(prefix = "oppdatering", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public final class TidsstyringKonfigurasjon {
}
