package no.nav.teamarbeidsforhold.githubapp.components;

import no.nav.teamarbeidsforhold.githubapp.naismanifest.NaisManifest;
import no.nav.teamarbeidsforhold.githubapp.qualifier.YamlParsing;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class ManifestParser {
    private final ObjectMapper objectMapper;

    public ManifestParser(@YamlParsing final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NaisManifest parse(final String innhold) {
        return objectMapper.readValue(innhold, NaisManifest.class);
    }

    public boolean hasScheduledDeploy(final String innhold) {
        final NaisManifest naisManifest = parse(innhold);
        if (naisManifest != null && naisManifest.hasScheduledDeploy()) {
            return true;
        }
        return containsScheduledDeploy(objectMapper.convertValue(objectMapper.readValue(innhold, Object.class), Object.class));
    }

    public boolean isScheduledDeploy(final String innhold) {
        return hasScheduledDeploy(innhold);
    }

    private boolean containsScheduledDeploy(final Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            final Object cron = map.get("cron");
            if (cron instanceof String cronValue && !cronValue.isBlank()) {
                return true;
            }
            final Object schedule = map.get("schedule");
            if (schedule != null && containsScheduledDeploy(schedule)) {
                return true;
            }
            for (final Object nested : map.values()) {
                if (containsScheduledDeploy(nested)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (final Object nested : iterable) {
                if (containsScheduledDeploy(nested)) {
                    return true;
                }
            }
        }
        return false;
    }
}
