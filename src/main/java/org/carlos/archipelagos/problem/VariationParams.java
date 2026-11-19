package org.carlos.archipelagos.problem;

import java.util.ArrayList;
import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;

/** Crossover/mutation parameters shared by all problems. */
public final class VariationParams {

    public static final String CROSSOVER_RATE = "crossoverRate";
    public static final String MUTATION_RATE = "mutationRate";

    private VariationParams() {
    }

    public static List<ParamSpec> with(List<ParamSpec> problemSpecific) {
        List<ParamSpec> specs = new ArrayList<>(problemSpecific);
        specs.add(ParamSpec.percentage(CROSSOVER_RATE, "probability that a pair of parents is recombined", 0.9));
        specs.add(ParamSpec.decimal(MUTATION_RATE, "per-gene mutation probability (0 = 1/genome length)", 0.0, 0.0, 1.0));
        return specs;
    }

    /** Per-gene mutation probability: the explicit value, or 1/n when 0. */
    public static double mutationRate(Params params, int genomeLength) {
        double rate = params.getDouble(MUTATION_RATE);
        return rate > 0 ? rate : 1.0 / Math.max(1, genomeLength);
    }
}
