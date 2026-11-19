package org.carlos.archipelagos.param;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validated, typed parameter values. Created by {@link #resolve(List, Map)} from the raw JSON
 * map of a request: unknown keys are rejected, defaults applied, numbers range-checked.
 */
public final class Params {

    private final Map<String, Object> values;

    private Params(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static Params resolve(List<ParamSpec> specs, Map<String, ?> raw) {
        Map<String, ?> input = raw == null ? Map.of() : raw;
        Map<String, ParamSpec> byName = new LinkedHashMap<>();
        for (ParamSpec spec : specs)
            byName.put(spec.name(), spec);
        for (String key : input.keySet())
            if (!byName.containsKey(key))
                throw new IllegalArgumentException("Unknown parameter '" + key + "', expected one of " + byName.keySet());
        Map<String, Object> values = new LinkedHashMap<>();
        for (ParamSpec spec : specs) {
            Object value = input.containsKey(spec.name()) ? input.get(spec.name()) : spec.defaultValue();
            if (value != null)
                values.put(spec.name(), convert(spec, value));
        }
        return new Params(values);
    }

    private static Object convert(ParamSpec spec, Object value) {
        try {
            Object converted = switch (spec.type()) {
            case INTEGER -> value instanceof Number n ? toExactInt(n) : Integer.parseInt(value.toString().trim());
            case DOUBLE, PERCENTAGE -> value instanceof Number n ? n.doubleValue() : Double.parseDouble(value.toString().trim());
            case BOOLEAN -> value instanceof Boolean b ? b : parseBoolean(value.toString());
            case STRING -> value.toString();
            case CHOICE -> {
                String s = value.toString();
                if (!spec.choices().contains(s))
                    throw new IllegalArgumentException("must be one of " + spec.choices());
                yield s;
            }
            };
            if (converted instanceof Number n) {
                double d = n.doubleValue();
                if (!Double.isFinite(d))
                    throw new IllegalArgumentException("must be finite");
                if (spec.min() != null && d < spec.min())
                    throw new IllegalArgumentException("must be >= " + spec.min());
                if (spec.max() != null && d > spec.max())
                    throw new IllegalArgumentException("must be <= " + spec.max());
            }
            return converted;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Parameter '" + spec.name() + "' (" + value + "): " + e.getMessage(), e);
        }
    }

    private static int toExactInt(Number n) {
        double d = n.doubleValue();
        if (d != Math.rint(d))
            throw new IllegalArgumentException("must be an integer");
        return Math.toIntExact((long) d);
    }

    private static boolean parseBoolean(String s) {
        return switch (s.trim().toLowerCase()) {
        case "true" -> true;
        case "false" -> false;
        default -> throw new IllegalArgumentException("must be true or false");
        };
    }

    public int getInt(String name) {
        return (Integer) require(name);
    }

    public double getDouble(String name) {
        return (Double) require(name);
    }

    public boolean getBoolean(String name) {
        return (Boolean) require(name);
    }

    public String getString(String name) {
        return (String) require(name);
    }

    public String getStringOrNull(String name) {
        Object value = values.get(name);
        return value == null ? null : value.toString();
    }

    public Map<String, Object> asMap() {
        return values;
    }

    private Object require(String name) {
        Object value = values.get(name);
        if (value == null)
            throw new IllegalArgumentException("Missing parameter '" + name + "'");
        return value;
    }
}
