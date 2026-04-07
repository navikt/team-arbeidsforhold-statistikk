package no.nav.teamarbeidsforhold.githubapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

@SpringBootApplication
public class TeamArbeidsforholdStatistikkApplication {
    static void main(String[] args) {
        final Map<String, String> miljø = System.getenv();
        miljø.keySet().stream().filter(s -> s.toLowerCase().contains("pg")).gather(Gatherers.windowFixed(20)).forEach(System.out::println);
        SpringApplication.run(TeamArbeidsforholdStatistikkApplication.class, args);
    }
}
