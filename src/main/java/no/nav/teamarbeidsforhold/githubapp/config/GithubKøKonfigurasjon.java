package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.worker.StoppSpringBootHvisTrådFeilerThreadFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class GithubKøKonfigurasjon {
    @Bean
    @Qualifier("github")
    public Executor enTrådForGithub(){
        return Executors.newSingleThreadExecutor(new StoppSpringBootHvisTrådFeilerThreadFactory());
    }
}
