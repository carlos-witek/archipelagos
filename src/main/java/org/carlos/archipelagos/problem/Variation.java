package org.carlos.archipelagos.problem;

import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;

/**
 * Recombination strategy of a problem (the 2008 {@code IStrategyRecombination}),
 * but operating on one pair / one child at a time; the engine does the pairing.
 */
public interface Variation<C extends Chromosome> {

    /** Probability that a pair of parents is recombined at all. */
    double crossoverRate();

    /** Recombine two children in place. */
    void crossover(C a, C b, RandomGenerator rng);

    /** Mutate one child in place. */
    void mutate(C c, RandomGenerator rng);
}
