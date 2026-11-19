package org.carlos.archipelagos.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.NondominatedSorting;

/** How arriving individuals enter an island. */
public enum ImmigrantPolicy {
    /** Each immigrant replaces one of the worst residents (last front, most crowded). */
    REPLACE_WORST,
    /** Each immigrant replaces a random resident. */
    REPLACE_RANDOM,
    /** Immigrants are simply added; the next survival step trims the population (2008 behaviour). */
    ADD;

    public <C extends Chromosome> List<C> integrate(List<C> population, List<C> immigrants, RandomGenerator rng) {
        List<C> next = new ArrayList<>(population);
        if (immigrants.isEmpty())
            return next;
        switch (this) {
        case ADD -> {
        }
        case REPLACE_RANDOM -> {
            for (int i = 0; i < immigrants.size() && !next.isEmpty(); i++)
                next.remove(rng.nextInt(next.size()));
        }
        case REPLACE_WORST -> {
            List<C> order = new ArrayList<>();
            for (List<C> front : NondominatedSorting.sort(next)) {
                NondominatedSorting.assignCrowding(front);
                List<C> sorted = new ArrayList<>(front);
                sorted.sort(Comparator.comparingDouble(Chromosome::crowding).reversed());
                order.addAll(sorted);
            }
            int remove = Math.min(immigrants.size(), order.size());
            next = new ArrayList<>(order.subList(0, order.size() - remove));
        }
        }
        next.addAll(immigrants);
        return next;
    }
}
