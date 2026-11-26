package no.nav.teamarbeidsforhold.githubapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeamArbeidsforholdStatistikkApplication {
    static void main(String[] args) {
        SpringApplication.run(TeamArbeidsforholdStatistikkApplication.class, args);
    }
}
