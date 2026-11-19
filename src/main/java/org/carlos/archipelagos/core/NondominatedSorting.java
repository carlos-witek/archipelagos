package org.carlos.archipelagos.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deb's fast non-dominated sorting and crowding distance (NSGA-II, 2002). */
public final class NondominatedSorting {

    private NondominatedSorting() {
    }

    /**
     * Splits the population into fronts F1, F2, ... using constrained domination
     * and stores the 0-based front index in {@link Chromosome#rank()}.
     */
    public static <C extends Chromosome> List<List<C>> sort(List<C> population) {
        int n = population.size();
        int[] dominatedBy = new int[n];
        List<List<Integer>> dominates = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            dominates.add(new ArrayList<>());
        List<List<C>> fronts = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int cmp = Dominance.compare(population.get(i), population.get(j));
                if (cmp < 0) {
                    dominates.get(i).add(j);
                    dominatedBy[j]++;
                } else if (cmp > 0) {
                    dominates.get(j).add(i);
                    dominatedBy[i]++;
                }
            }
        }
        for (int i = 0; i < n; i++)
            if (dominatedBy[i] == 0)
                current.add(i);
        int rank = 0;
        while (!current.isEmpty()) {
            List<C> front = new ArrayList<>(current.size());
            List<Integer> next = new ArrayList<>();
            for (int i : current) {
                C c = population.get(i);
                c.setRank(rank);
                front.add(c);
                for (int j : dominates.get(i))
                    if (--dominatedBy[j] == 0)
                        next.add(j);
            }
            fronts.add(front);
            current = next;
            rank++;
        }
        return fronts;
    }

    /** Assigns crowding distance within one front (boundary points get +infinity). */
    public static <C extends Chromosome> void assignCrowding(List<C> front) {
        int size = front.size();
        for (C c : front)
            c.setCrowding(0.0);
        if (size == 0)
            return;
        if (size <= 2) {
            for (C c : front)
                c.setCrowding(Double.POSITIVE_INFINITY);
            return;
        }
        int m = front.get(0).objectiveCount();
        List<C> sorted = new ArrayList<>(front);
        for (int k = 0; k < m; k++) {
            final int objective = k;
            sorted.sort(Comparator.comparingDouble(c -> c.objective(objective)));
            double min = sorted.get(0).objective(k);
            double max = sorted.get(size - 1).objective(k);
            sorted.get(0).setCrowding(Double.POSITIVE_INFINITY);
            sorted.get(size - 1).setCrowding(Double.POSITIVE_INFINITY);
            double range = max - min;
            if (range <= 0)
                continue;
            for (int i = 1; i < size - 1; i++) {
                C c = sorted.get(i);
                if (!Double.isInfinite(c.crowding()))
                    c.setCrowding(c.crowding() + (sorted.get(i + 1).objective(k) - sorted.get(i - 1).objective(k)) / range);
            }
        }
    }
}
