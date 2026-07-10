package no.nav.teamarbeidsforhold.githubapp.dto;

public record Manifest(String content) {
    private static Manifest of(final String type, final String jobname) {
        return new Manifest("apiVersion: nais.io/v1alpha1\nkind: " + type + "\nmetadata:\n  labels:\n    team: arbeidsforhold\n  name: " + jobname + "\n");
    }

    public static Manifest job(final String jobname) {
        return of("Naisjob", jobname);
    }

    public static Manifest application(final String appname) {
        return Manifest.of("Application", appname);
    }
}
