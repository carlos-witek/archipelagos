package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.NondominatedSorting;

/**
 * NSGA-II (Deb et al. 2002), the modern default: binary tournaments on (front, crowding distance)
 * and elitist (mu+lambda) survival. Not part of the 2008 thesis.
 */
public final class Nsga2 implements Selection {

    private static final Comparator<Chromosome> CROWDED = Comparator.comparingInt(Chromosome::rank)
            .thenComparing(Comparator.comparingDouble(Chromosome::crowding).reversed());

    @Override
    public boolean elitist() {
        return true;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        for (List<C> front : NondominatedSorting.sort(population))
            NondominatedSorting.assignCrowding(front);
        RandomGenerator rng = ctx.rng();
        int n = population.size();
        List<C> pool = new ArrayList<>(count);
        while (pool.size() < count) {
            C a = population.get(rng.nextInt(n));
            C b = population.get(rng.nextInt(n));
            int cmp = CROWDED.compare(a, b);
            pool.add(cmp < 0 ? a : cmp > 0 ? b : (rng.nextBoolean() ? a : b));
        }
        return pool;
    }

    @Override
    public <C extends Chromosome> List<C> survive(List<C> population, List<C> offspring, int size,
            SelectionContext ctx) {
        List<C> union = new ArrayList<>(population.size() + offspring.size());
        union.addAll(population);
        union.addAll(offspring);
        List<C> next = new ArrayList<>(size);
        for (List<C> front : NondominatedSorting.sort(union)) {
            NondominatedSorting.assignCrowding(front);
            if (next.size() + front.size() <= size) {
                next.addAll(front);
            } else {
                List<C> sorted = new ArrayList<>(front);
                sorted.sort(Comparator.comparingDouble(Chromosome::crowding).reversed());
                next.addAll(sorted.subList(0, size - next.size()));
            }
            if (next.size() >= size)
                break;
        }
        return next;
    }
}
