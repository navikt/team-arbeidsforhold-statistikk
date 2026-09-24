package no.nav.teamarbeidsforhold.githubapp.naismanifest;

public record NaisManifest(String kind, Spec spec) {
    public boolean hasScheduledDeploy() {
        return spec != null && spec.hasScheduledDeploy();
    }

    public boolean isScheduledDeploy() {
        return hasScheduledDeploy();
    }
}
