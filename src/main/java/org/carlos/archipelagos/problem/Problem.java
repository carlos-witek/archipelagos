package org.carlos.archipelagos.problem;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;

/**
 * A configured optimisation problem: genotype factory (initialisation), evaluation
 * (fitness) and variation (recombination). All objectives are minimised.
 * <p>
 * Implementations must be thread-safe: islands evaluate concurrently against one instance.
 */
public interface Problem<C extends Chromosome> {

    String name();

    List<String> objectives();

    /** A new random individual (the 2008 {@code IStrategyInitialization}). */
    C create(RandomGenerator rng);

    /**
     * Computes all objectives and the constraint violation from scratch
     * (the 2008 {@code IStrategyFitness}, which in places left stale penalties behind).
     */
    void evaluate(C c);

    Variation<C> variation();

    /** True Pareto front (or a dense approximation) if one is known, for GD/IGD/HV. */
    default Optional<List<double[]>> referenceFront() {
        return Optional.empty();
    }
}
