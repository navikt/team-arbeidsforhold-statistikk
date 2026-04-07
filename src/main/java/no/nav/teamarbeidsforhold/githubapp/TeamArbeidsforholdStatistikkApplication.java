package no.nav.teamarbeidsforhold.githubapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
public class TeamArbeidsforholdStatistikkApplication {
	static void main(String[] args) {
		final Map<String, String> miljø = System.getenv();
		System.out.println(String.join(",", miljø.keySet()));
		SpringApplication.run(TeamArbeidsforholdStatistikkApplication.class, args);
	}
}
