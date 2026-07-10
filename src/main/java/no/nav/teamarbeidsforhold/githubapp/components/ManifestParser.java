package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.qualifier.YamlParsing;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ManifestParser {
    private final ObjectMapper objectMapper;

    public ManifestParser(@YamlParsing final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NaisManifest parse(final String innhold) {
        return objectMapper.readValue(innhold, NaisManifest.class);
    }
}
