package no.nav.teamarbeidsforhold.githubapp.naismanifest;

import java.util.List;
import java.util.Map;

public record Spec(List<String> ingresses, Object schedule, Object on) {
    public Spec {
        ingresses = ingresses == null ? List.of() : ingresses;
    }

    public Spec(final List<String> ingresses) {
        this(ingresses, null, null);
    }

    public boolean hasScheduledDeploy() {
        return scheduleExpression() != null;
    }

    public boolean isScheduledDeploy() {
        return hasScheduledDeploy();
    }

    public String scheduleExpression() {
        final String scheduleValue = extractScheduleExpression(schedule);
        if (scheduleValue != null) {
            return scheduleValue;
        }
        return extractScheduleExpression(on);
    }

    private static String extractScheduleExpression(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String schedule) {
            return schedule.isBlank() ? null : schedule;
        }
        if (value instanceof Map<?, ?> map) {
            final Object cron = map.get("cron");
            if (cron instanceof String cronValue && !cronValue.isBlank()) {
                return cronValue;
            }
            final Object scheduleValue = map.get("schedule");
            if (scheduleValue != null) {
                final String scheduleCron = extractScheduleExpression(scheduleValue);
                if (scheduleCron != null) {
                    return scheduleCron;
                }
            }
            for (final Object nested : map.values()) {
                final String nestedCron = extractScheduleExpression(nested);
                if (nestedCron != null) {
                    return nestedCron;
                }
            }
            return null;
        }
        if (value instanceof Iterable<?> iterable) {
            for (final Object nested : iterable) {
                final String nestedCron = extractScheduleExpression(nested);
                if (nestedCron != null) {
                    return nestedCron;
                }
            }
        }
        return null;
    }
}
