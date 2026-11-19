package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;

/** Shared helpers for the classic schemes. */
public final class Selections {

    private Selections() {
    }

    /**
     * Fitness-proportional selection with replacement using the {@code fitness} scratch value
     * (higher is better). Binary search over the cumulative sum instead of the 2008 linear scan;
     * falls back to uniform selection when the total is zero or not finite (2008: NaN poisoned the wheel).
     */
    public static <C extends Chromosome> List<C> roulette(List<C> pool, int count, RandomGenerator rng) {
        int n = pool.size();
        double[] cumulative = new double[n];
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            double f = pool.get(i).fitness();
            total += (Double.isFinite(f) && f > 0) ? f : 0.0;
            cumulative[i] = total;
        }
        List<C> selected = new ArrayList<>(count);
        boolean uniform = !(total > 0) || !Double.isFinite(total);
        for (int s = 0; s < count; s++) {
            if (uniform) {
                selected.add(pool.get(rng.nextInt(n)));
                continue;
            }
            double ball = rng.nextDouble() * total;
            int lo = 0;
            int hi = n - 1;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (cumulative[mid] > ball)
                    hi = mid;
                else
                    lo = mid + 1;
            }
            selected.add(pool.get(lo));
        }
        return selected;
    }

    /**
     * Fitness of proportional schemes for infeasible individuals: kept far below every feasible
     * one and decreasing with the violation (replaces the 2008 {@code fitness * (1 - penalty)}).
     */
    public static double constrain(double fitness, Chromosome c) {
        if (c.isFeasible())
            return fitness;
        return 1e-9 * fitness / (1.0 + c.violation());
    }

    /**
     * The 2008 environmental selection for all classic schemes: drop random members of the
     * population until offspring fit, then add all offspring.
     */
    public static <C extends Chromosome> List<C> randomReplacement(List<C> population, List<C> offspring, int size,
            RandomGenerator rng) {
        List<C> next = new ArrayList<>(population);
        int keep = Math.max(0, size - offspring.size());
        while (next.size() > keep)
            next.remove(rng.nextInt(next.size()));
        next.addAll(offspring.subList(0, Math.min(offspring.size(), size)));
        return next;
    }

    /** Splits {@code total} into {@code parts} sizes differing by at most one. */
    public static int[] split(int total, int parts) {
        int[] sizes = new int[parts];
        for (int i = 0; i < parts; i++)
            sizes[i] = total / parts + (i < total % parts ? 1 : 0);
        return sizes;
    }
}
