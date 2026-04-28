package no.nav.teamarbeidsforhold.githubapp.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public final class StoppSpringBoot {
    private static ConfigurableApplicationContext context;

    StoppSpringBoot(ConfigurableApplicationContext context) {
        StoppSpringBoot.context = context;
    }

    public static void feil(final Throwable exception) {
        System.exit(SpringApplication.exit(context, () -> 100));
    }
}
