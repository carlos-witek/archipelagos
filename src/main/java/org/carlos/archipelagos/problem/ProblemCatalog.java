package org.carlos.archipelagos.problem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.carlos.archipelagos.param.Params;
import org.springframework.stereotype.Component;

/** All problem factories found by Spring, by name. */
@Component
public class ProblemCatalog {

    private final Map<String, ProblemFactory> factories = new LinkedHashMap<>();

    public ProblemCatalog(List<ProblemFactory> factories) {
        for (ProblemFactory factory : factories)
            if (this.factories.put(factory.name(), factory) != null)
                throw new IllegalStateException("Duplicate problem name " + factory.name());
    }

    public List<ProblemFactory> factories() {
        return List.copyOf(factories.values());
    }

    public ProblemFactory get(String name) {
        ProblemFactory factory = factories.get(name);
        if (factory == null)
            throw new IllegalArgumentException("Unknown problem '" + name + "', expected one of " + factories.keySet());
        return factory;
    }

    public Problem<?> create(String name, Map<String, Object> rawParams) {
        ProblemFactory factory = get(name);
        return factory.create(Params.resolve(factory.params(), rawParams));
    }
}
