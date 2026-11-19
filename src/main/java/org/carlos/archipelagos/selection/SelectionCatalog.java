package org.carlos.archipelagos.selection;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.springframework.stereotype.Component;

/**
 * All selection schemes with their parameters, ordered by the year the scheme was published.
 * Each island gets its own instance.
 */
@Component
public class SelectionCatalog {

    /** Default scheme when a run request names none. */
    public static final String DEFAULT = "nsga2";

    /** @param year year the scheme was first published */
    public record Entry(String name, int year, String description, List<ParamSpec> params,
            Function<Params, Selection> factory) {
    }

    private static final ParamSpec SHARING = ParamSpec.decimal("sigmaShare",
            "niche radius in normalised objective space", 0.1, 1e-6, 10.0);
    private static final ParamSpec VECTORS = ParamSpec.string("weightVectors",
            "weight vectors 'w1,w2;w1,w2;...' (empty = evenly spaced)", "0.1,0.9;0.3,0.7;0.5,0.5;0.7,0.3;0.9,0.1");

    private static final ParamSpec NEIGHBOUR = ParamSpec.integer("k",
            "k-th nearest neighbour for the density estimate (0 = square root of the set size)", 0, 0, 10000);

    private final Map<String, Entry> entries = new java.util.LinkedHashMap<>();

    public SelectionCatalog() {
        register(new Entry("vega", 1985, "VEGA (Schaffer 1985): one share of the mating pool per objective.",
                List.of(), p -> new Vega()));
        register(new Entry("random-objective", 1991,
                "Random objective (Kursawe 1991): one randomly chosen objective drives each generation.", List.of(),
                p -> new RandomObjectiveSelection()));
        register(new Entry("wbga-ve", 1992, "Weight-based GA (Hajela & Lin 1992), one sub-pool per weight vector.",
                List.of(VECTORS), p -> new WbgaVe(p.getString("weightVectors"))));
        register(new Entry("wbga-sf", 1992,
                "Weight-based GA (Hajela & Lin 1992), individuals grouped by weight vector, group sharing.",
                List.of(VECTORS), p -> new WbgaSf(p.getString("weightVectors"))));
        register(new Entry("moga", 1993, "MOGA (Fonseca & Fleming 1993): dominator-count ranks + sharing.",
                List.of(SHARING), p -> new Moga(p.getDouble("sigmaShare"))));
        register(new Entry("npga", 1994, "NPGA (Horn, Nafpliotis & Goldberg 1994): Pareto domination tournaments.",
                List.of(SHARING, ParamSpec.integer("comparisonSetSize", "size of the comparison set", 5, 1, 1000)),
                p -> new Npga(p.getDouble("sigmaShare"), p.getInt("comparisonSetSize"))));
        register(new Entry("nsga", 1994, "NSGA (Srinivas & Deb 1994): non-dominated fronts + sharing.",
                List.of(SHARING), p -> new Nsga(p.getDouble("sigmaShare"))));
        register(new Entry("spea", 1999, "SPEA (Zitzler & Thiele 1999): external non-dominated set with clustering.",
                List.of(ParamSpec.integer("externalSize", "maximum size of the external set", 8, 1, 10000)),
                p -> new Spea(p.getInt("externalSize"))));
        register(new Entry("spea2", 2001,
                "SPEA2 (Zitzler, Laumanns & Thiele 2001): strength + k-nearest-neighbour density, "
                        + "fixed-size archive with boundary-preserving truncation.",
                List.of(NEIGHBOUR), p -> new Spea2(p.getInt("k"), false)));
        register(new Entry(DEFAULT, 2002,
                "NSGA-II (Deb et al. 2002): crowded tournaments + elitist survival. Modern default.", List.of(),
                p -> new Nsga2()));
        register(new Entry("nsga3", 2014,
                "NSGA-III (Deb & Jain 2014): non-dominated sorting + niching around reference directions. "
                        + "For 4 or more objectives.",
                List.of(ParamSpec.integer("divisions",
                        "reference-point divisions per objective (0 = as many points as fit the population)", 0, 0,
                        1000)),
                p -> new Nsga3(p.getInt("divisions"))));
        register(new Entry("spea2-sde", 2014,
                "SPEA2+SDE (Li, Yang & Liu 2014): SPEA2 with shift-based density, for many objectives.",
                List.of(NEIGHBOUR), p -> new Spea2(p.getInt("k"), true)));
    }

    private void register(Entry entry) {
        Entry last = entries.isEmpty() ? null : List.copyOf(entries.values()).get(entries.size() - 1);
        if (last != null && last.year() > entry.year())
            throw new IllegalStateException("Selection schemes must be registered in chronological order: "
                    + entry.name() + " (" + entry.year() + ") after " + last.name() + " (" + last.year() + ")");
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
