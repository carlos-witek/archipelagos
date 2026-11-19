package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * SPEA (Zitzler & Thiele 1999) with its external non-dominated set P'.
 * <ol>
 * <li>P' is updated with the non-dominated members of P; members of P' dominated by others are
 * removed (2008 never removed them).</li>
 * <li>If |P'| exceeds the limit it is reduced by average-linkage clustering, keeping the most
 * central member of each cluster (2008 used a niche count and left {@code ClusterKey} unused).</li>
 * <li>Strength of i in P' = (number of P members it covers) / (|P| + 1); fitness of j in P =
 * 1 + sum of strengths of the P' members covering j. Lower is better.</li>
 * <li>Binary tournaments over P + P' fill the mating pool (2008 used a walking-index scheme that
 * crashed for populations under 10).</li>
 * </ol>
 * The external set is per island and survives between generations.
 */
public final class Spea extends ClassicSelection {

    private final int maxExternalSize;
    private final List<Chromosome> external = new ArrayList<>();

    public Spea(int maxExternalSize) {
        this.maxExternalSize = maxExternalSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        updateExternal(population);
        // strengths and fitness
        int n = population.size();
        for (C c : population)
            c.setFitness(1.0);
        for (Chromosome e : external) {
            int covered = 0;
            for (C c : population)
                if (Dominance.covers(e, c))
                    covered++;
            double strength = (double) covered / (n + 1);
            e.setFitness(strength);
            for (C c : population)
                if (Dominance.covers(e, c))
                    c.setFitness(c.fitness() + strength);
        }
        List<C> union = new ArrayList<>(population);
        for (Chromosome e : external)
            union.add((C) e);
        RandomGenerator rng = ctx.rng();
        List<C> pool = new ArrayList<>(count);
        while (pool.size() < count) {
            C a = union.get(rng.nextInt(union.size()));
            C b = union.get(rng.nextInt(union.size()));
            pool.add(a.fitness() <= b.fitness() ? a : b);
        }
        return pool;
    }

    private void updateExternal(List<? extends Chromosome> population) {
        List<Chromosome> candidates = new ArrayList<>(external);
        for (Chromosome c : population) {
            boolean dominated = false;
            for (Chromosome other : population)
                if (Dominance.dominates(other, c)) {
                    dominated = true;
                    break;
                }
            if (!dominated)
                candidates.add(c.copy());
        }
        external.clear();
        outer: for (Chromosome c : candidates) {
            for (Chromosome other : candidates)
                if (other != c && Dominance.dominates(other, c))
                    continue outer;
            for (Chromosome kept : external)
                if (java.util.Arrays.equals(kept.objectives(), c.objectives()))
                    continue outer;
            external.add(c);
        }
        if (external.size() > maxExternalSize)
            reduceByClustering();
    }

    private void reduceByClustering() {
        ObjectiveSpace space = ObjectiveSpace.of(external);
        List<List<Chromosome>> clusters = new ArrayList<>();
        for (Chromosome c : external)
            clusters.add(new ArrayList<>(List.of(c)));
        while (clusters.size() > maxExternalSize) {
            int bestI = 0;
            int bestJ = 1;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < clusters.size(); i++)
                for (int j = i + 1; j < clusters.size(); j++) {
                    double d = averageDistance(clusters.get(i), clusters.get(j), space);
                    if (d < best) {
                        best = d;
                        bestI = i;
                        bestJ = j;
                    }
                }
            clusters.get(bestI).addAll(clusters.remove(bestJ));
        }
        external.clear();
        for (List<Chromosome> cluster : clusters) {
            Chromosome centroid = cluster.get(0);
            double best = Double.POSITIVE_INFINITY;
            for (Chromosome c : cluster) {
                double sum = 0.0;
                for (Chromosome other : cluster)
                    sum += space.distance(c, other);
                if (sum < best) {
                    best = sum;
                    centroid = c;
                }
            }
            external.add(centroid);
        }
    }

    private static double averageDistance(List<Chromosome> a, List<Chromosome> b, ObjectiveSpace space) {
        double sum = 0.0;
        for (Chromosome x : a)
            for (Chromosome y : b)
                sum += space.distance(x, y);
        return sum / (a.size() * b.size());
    }

    /** Visible for tests. */
    List<Chromosome> external() {
        return external;
    }
}
