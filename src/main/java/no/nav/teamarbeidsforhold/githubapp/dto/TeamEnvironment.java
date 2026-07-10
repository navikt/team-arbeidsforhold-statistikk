package no.nav.teamarbeidsforhold.githubapp.dto;

public record TeamEnvironment(Environment environment) {
    public static TeamEnvironment of(final String name) {
        return new TeamEnvironment(new Environment(name));
    }
}
