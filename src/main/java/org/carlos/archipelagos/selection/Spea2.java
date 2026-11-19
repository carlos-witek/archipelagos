package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * SPEA2 (Zitzler, Laumanns & Thiele 2001), optionally with shift-based density estimation
 * (SPEA2+SDE, Li, Yang & Liu 2014).
 * <p>
 * The population the engine keeps between generations plays the role of SPEA2's fixed-size
 * archive; each generation's offspring are the new "population" P.
 * <ul>
 * <li>Fitness F(i) = R(i) + D(i). Strength S(i) = number of solutions i dominates; raw fitness
 * R(i) = sum of the strengths of everything dominating i (0 for non-dominated solutions);
 * density D(i) = 1 / (sigma_k + 2), sigma_k = distance to the k-th nearest neighbour. Lower is better,
 * and F &lt; 1 exactly for non-dominated solutions.</li>
 * <li>Environmental selection keeps all non-dominated solutions of archive + offspring. Too few: fill
 * with the best dominated ones by F. Too many: repeatedly remove the solution whose sorted list of
 * neighbour distances is lexicographically smallest, which preserves the boundary points.</li>
 * <li>Mating selection: binary tournaments on F.</li>
 * </ul>
 * Distances are measured in objective space normalised to the current set (the 2001 paper used raw
 * objectives). With SDE, the distance from p to q is computed after shifting q to max(q, p) in every
 * objective, so a neighbour only counts as crowding p in the objectives where it is worse than p;
 * this keeps the density estimate meaningful with many objectives, where nearly every solution is
 * non-dominated.
 */
public final class Spea2 implements Selection {

    private final int k;
    private final boolean shiftBasedDensity;

    /**
     * @param k                 nearest-neighbour index for the density; 0 = sqrt(set size) as in the paper
     * @param shiftBasedDensity true for SPEA2+SDE
     */
    public Spea2(int k, boolean shiftBasedDensity) {
        this.k = k;
        this.shiftBasedDensity = shiftBasedDensity;
    }

    @Override
    public boolean elitist() {
        return true;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        assignFitness(population, distanceMatrix(population));
        RandomGenerator rng = ctx.rng();
        int n = population.size();
        List<C> pool = new ArrayList<>(count);
        while (pool.size() < count) {
            C a = population.get(rng.nextInt(n));
            C b = population.get(rng.nextInt(n));
            pool.add(a.fitness() < b.fitness() ? a : b.fitness() < a.fitness() ? b : (rng.nextBoolean() ? a : b));
        }
        return pool;
    }

    @Override
    public <C extends Chromosome> List<C> survive(List<C> population, List<C> offspring, int size,
            SelectionContext ctx) {
        List<C> union = new ArrayList<>(population.size() + offspring.size());
        union.addAll(population);
        union.addAll(offspring);
        double[][] distances = distanceMatrix(union);
        assignFitness(union, distances);

        List<Integer> nondominated = new ArrayList<>();
        List<Integer> dominated = new ArrayList<>();
        for (int i = 0; i < union.size(); i++)
            (union.get(i).fitness() < 1.0 ? nondominated : dominated).add(i);

        List<Integer> chosen;
        if (nondominated.size() <= size) {
            chosen = new ArrayList<>(nondominated);
            dominated.sort((a, b) -> Double.compare(union.get(a).fitness(), union.get(b).fitness()));
            chosen.addAll(dominated.subList(0, Math.min(dominated.size(), size - chosen.size())));
        } else {
            chosen = truncate(nondominated, distances, size);
        }
        List<C> next = new ArrayList<>(chosen.size());
        for (int i : chosen)
            next.add(union.get(i));
        return next;
    }

    /** F(i) = R(i) + D(i), stored in {@link Chromosome#fitness()}. */
    private <C extends Chromosome> void assignFitness(List<C> set, double[][] distances) {
        int n = set.size();
        boolean[][] dominates = new boolean[n][n];
        int[] strength = new int[n];
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                int cmp = Dominance.compare(set.get(i), set.get(j));
                if (cmp < 0) {
                    dominates[i][j] = true;
                    strength[i]++;
                } else if (cmp > 0) {
                    dominates[j][i] = true;
                    strength[j]++;
                }
            }
        int kth = k > 0 ? Math.min(k, n - 1) : Math.max(1, Math.min(n - 1, (int) Math.sqrt(n)));
        for (int i = 0; i < n; i++) {
            double raw = 0.0;
            for (int j = 0; j < n; j++)
                if (dominates[j][i])
                    raw += strength[j];
            double density = 0.0;
            if (n > 1) {
                double[] row = new double[n - 1];
                for (int j = 0, r = 0; j < n; j++)
                    if (j != i)
                        row[r++] = distances[i][j];
                Arrays.sort(row);
                density = 1.0 / (row[kth - 1] + 2.0);
            }
            set.get(i).setFitness(raw + density);
        }
    }

    /**
     * Distance matrix in normalised objective space: row i holds the distances as seen from i
     * (asymmetric with SDE).
     */
    private double[][] distanceMatrix(List<? extends Chromosome> set) {
        int n = set.size();
        double[][] d = new double[n][n];
        if (n == 0)
            return d;
        ObjectiveSpace space = ObjectiveSpace.of(set);
        int m = space.dimensions();
        double[][] f = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int o = 0; o < m; o++)
                f[i][o] = space.normalized(set.get(i), o);
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                if (i == j)
                    continue;
                double sum = 0.0;
                for (int o = 0; o < m; o++) {
                    double diff = shiftBasedDensity ? Math.max(0.0, f[j][o] - f[i][o]) : f[j][o] - f[i][o];
                    sum += diff * diff;
                }
                d[i][j] = Math.sqrt(sum);
            }
        return d;
    }

    /**
     * Archive truncation: while too large, remove the member whose ascending list of distances to
     * the remaining members is lexicographically smallest.
     */
    private static List<Integer> truncate(List<Integer> members, double[][] distances, int size) {
        int n = members.size();
        // sorted distance list of each member to every other member, shrunk as members are removed
        double[][] sorted = new double[n][];
        int[] length = new int[n];
        for (int a = 0; a < n; a++) {
            double[] row = new double[n - 1];
            for (int b = 0, r = 0; b < n; b++)
                if (b != a)
                    row[r++] = distances[members.get(a)][members.get(b)];
            Arrays.sort(row);
            sorted[a] = row;
            length[a] = n - 1;
        }
        boolean[] removed = new boolean[n];
        for (int remaining = n; remaining > size; remaining--) {
            int victim = -1;
            for (int a = 0; a < n; a++)
                if (!removed[a] && (victim < 0 || lexicographicallySmaller(sorted[a], length[a], sorted[victim],
                        length[victim])))
                    victim = a;
            removed[victim] = true;
            for (int a = 0; a < n; a++) {
                if (removed[a])
                    continue;
                double gone = distances[members.get(a)][members.get(victim)];
                int at = Arrays.binarySearch(sorted[a], 0, length[a], gone);
                System.arraycopy(sorted[a], at + 1, sorted[a], at, length[a] - at - 1);
                length[a]--;
            }
        }
        List<Integer> kept = new ArrayList<>(size);
        for (int a = 0; a < n; a++)
            if (!removed[a])
                kept.add(members.get(a));
        return kept;
    }

    private static boolean lexicographicallySmaller(double[] x, int xLength, double[] y, int yLength) {
        int length = Math.min(xLength, yLength);
        for (int i = 0; i < length; i++) {
            if (x[i] < y[i])
                return true;
            if (x[i] > y[i])
                return false;
        }
        return false;
    }
}
