package no.nav.teamarbeidsforhold.githubapp.naisapi.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record Image(String name, String tag, List<Vulnerability> vulnerabilities) {
    public @Size(max = 250) String navnMedTag() {
        return name() + "@" + tag();
    }
}
