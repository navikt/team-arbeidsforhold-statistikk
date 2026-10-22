package no.nav.teamarbeidsforhold.githubapp.config;

import com.slack.api.Slack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackKonfigurasjon {
    @Bean
    Slack slack() {
        return Slack.getInstance();
    }
}
