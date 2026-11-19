package org.carlos.archipelagos.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.NondominatedSorting;

/**
 * Which individuals leave an island (2008 {@code MigrationSimpleBest/Random/Contest}).
 * All policies draw <b>without replacement</b>; 2008 drew with replacement and could send
 * the same individual twice.
 */
public enum EmigrantPolicy {
    /** A random sample of the island's non-dominated individuals. */
    BEST,
    /** A uniform random sample. */
    RANDOM,
    /** Binary Pareto tournaments. */
    TOURNAMENT;

    public <C extends Chromosome> List<C> choose(List<C> population, int count, RandomGenerator rng) {
        List<C> candidates = switch (this) {
        case BEST -> new ArrayList<>(NondominatedSorting.sort(population).get(0));
        case RANDOM, TOURNAMENT -> new ArrayList<>(population);
        };
        List<C> chosen = new ArrayList<>(count);
        while (chosen.size() < count && !candidates.isEmpty()) {
            int i = rng.nextInt(candidates.size());
            if (this == TOURNAMENT && candidates.size() > 1) {
                int j = rng.nextInt(candidates.size() - 1);
                if (j >= i)
                    j++;
                int cmp = Dominance.compare(candidates.get(i), candidates.get(j));
                if (cmp > 0 || (cmp == 0 && rng.nextBoolean()))
                    i = j;
            }
            chosen.add(candidates.remove(i));
        }
        return chosen;
    }
}
