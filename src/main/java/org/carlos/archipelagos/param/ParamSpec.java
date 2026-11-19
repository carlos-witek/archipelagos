package org.carlos.archipelagos.param;

import java.util.List;

/**
 * Declarative description of one strategy parameter, published through the catalog
 * endpoint so a UI can build its form from it.
 * <p>
 * This is the successor of the 2008 {@code @Input}/{@code @Default}/{@code @Values}
 * annotations: the same idea, but the spec and the value now live in one place, so the
 * two cannot disagree (in 2008 {@code @Default("0.89")} sat next to a field set to 0.90).
 *
 * @param defaultValue may be {@code null} for an optional parameter without a default
 */
public record ParamSpec(String name, ParamType type, String label, Object defaultValue, Double min, Double max,
        List<String> choices) {

    public static ParamSpec integer(String name, String label, int defaultValue, int min, int max) {
        return new ParamSpec(name, ParamType.INTEGER, label, defaultValue, (double) min, (double) max, List.of());
    }

    public static ParamSpec decimal(String name, String label, double defaultValue) {
        return new ParamSpec(name, ParamType.DOUBLE, label, defaultValue, null, null, List.of());
    }

    public static ParamSpec decimal(String name, String label, double defaultValue, double min, double max) {
        return new ParamSpec(name, ParamType.DOUBLE, label, defaultValue, min, max, List.of());
    }

    public static ParamSpec percentage(String name, String label, double defaultValue) {
        return new ParamSpec(name, ParamType.PERCENTAGE, label, defaultValue, 0.0, 1.0, List.of());
    }

    public static ParamSpec bool(String name, String label, boolean defaultValue) {
        return new ParamSpec(name, ParamType.BOOLEAN, label, defaultValue, null, null, List.of());
    }

    public static ParamSpec string(String name, String label, String defaultValue) {
        return new ParamSpec(name, ParamType.STRING, label, defaultValue, null, null, List.of());
    }

    public static ParamSpec choice(String name, String label, String defaultValue, List<String> choices) {
        return new ParamSpec(name, ParamType.CHOICE, label, defaultValue, null, null, List.copyOf(choices));
    }
}
