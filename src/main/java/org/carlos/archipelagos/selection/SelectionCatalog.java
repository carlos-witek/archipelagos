package org.carlos.archipelagos.selection;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.springframework.stereotype.Component;

/** All selection schemes with their parameters. Each island gets its own instance. */
@Component
public class SelectionCatalog {

    public record Entry(String name, String description, List<ParamSpec> params, Function<Params, Selection> factory) {
    }

    private static final ParamSpec SHARING = ParamSpec.decimal("sigmaShare",
            "niche radius in normalised objective space", 0.1, 1e-6, 10.0);
    private static final ParamSpec VECTORS = ParamSpec.string("weightVectors",
            "weight vectors 'w1,w2;w1,w2;...' (empty = evenly spaced)", "0.1,0.9;0.3,0.7;0.5,0.5;0.7,0.3;0.9,0.1");

    private final Map<String, Entry> entries = new java.util.LinkedHashMap<>();

    public SelectionCatalog() {
        register(new Entry("nsga2", "NSGA-II (Deb 2002): crowded tournaments + elitist survival. Modern default.",
                List.of(), p -> new Nsga2()));
        register(new Entry("vega", "VEGA (Schaffer 1985): one share of the mating pool per objective.", List.of(),
                p -> new Vega()));
        register(new Entry("moga", "MOGA (Fonseca & Fleming 1993): dominator-count ranks + sharing.", List.of(SHARING),
                p -> new Moga(p.getDouble("sigmaShare"))));
        register(new Entry("nsga", "NSGA (Srinivas & Deb 1994): non-dominated fronts + sharing.", List.of(SHARING),
                p -> new Nsga(p.getDouble("sigmaShare"))));
        register(new Entry("npga", "NPGA (Horn 1994): Pareto domination tournaments.",
                List.of(SHARING, ParamSpec.integer("comparisonSetSize", "size of the comparison set", 5, 1, 1000)),
                p -> new Npga(p.getDouble("sigmaShare"), p.getInt("comparisonSetSize"))));
        register(new Entry("spea", "SPEA (Zitzler & Thiele 1999): external non-dominated set with clustering.",
                List.of(ParamSpec.integer("externalSize", "maximum size of the external set", 8, 1, 10000)),
                p -> new Spea(p.getInt("externalSize"))));
        register(new Entry("wbga-ve", "Weight-based GA, one sub-pool per weight vector.", List.of(VECTORS),
                p -> new WbgaVe(p.getString("weightVectors"))));
        register(new Entry("wbga-sf", "Weight-based GA, individuals grouped by weight vector, group sharing.",
                List.of(VECTORS), p -> new WbgaSf(p.getString("weightVectors"))));
        register(new Entry("random-objective", "One random objective drives each generation.", List.of(),
                p -> new RandomObjectiveSelection()));
    }

    private void register(Entry entry) {
        entries.put(entry.name(), entry);
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public Entry get(String name) {
        Entry entry = entries.get(name);
        if (entry == null)
            throw new IllegalArgumentException("Unknown selection '" + name + "', expected one of " + entries.keySet());
        return entry;
    }
}
