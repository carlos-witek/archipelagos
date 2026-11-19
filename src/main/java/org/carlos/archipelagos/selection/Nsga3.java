package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.NondominatedSorting;
import org.carlos.archipelagos.core.Simplex;

/**
 * NSGA-III (Deb & Jain 2014): NSGA-II's non-dominated sorting, with crowding distance replaced by
 * niching around a set of evenly spaced reference directions. Designed for many objectives
 * (4 and more), where crowding distance stops distinguishing solutions.
 * <p>
 * Survival: the fronts of parents + offspring are taken whole while they fit. Members of the front
 * that overflows are chosen one at a time:
 * <ol>
 * <li>Normalise the objectives: subtract the ideal point, find one extreme point per axis (minimum
 * achievement scalarising function), and divide by the intercepts of the hyperplane through them
 * (falling back to the nadir of the first front when that plane is degenerate).</li>
 * <li>Associate every candidate with its nearest reference direction (perpendicular distance).</li>
 * <li>Repeatedly pick a least-crowded direction (fewest already-selected members); take its closest
 * candidate if it has none yet, otherwise a random one.</li>
 * </ol>
 * Mating selection: binary tournaments that prefer the dominating (or feasible) individual and are
 * otherwise random, as in the constrained NSGA-III of Jain &amp; Deb.
 */
public final class Nsga3 implements Selection {

    private final int divisions;
    private List<double[]> references;

    /** @param divisions reference-point divisions per objective; 0 = the largest giving no more points than the population */
    public Nsga3(int divisions) {
        this.divisions = divisions;
    }

    @Override
    public boolean elitist() {
        return true;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        RandomGenerator rng = ctx.rng();
        int n = population.size();
        List<C> pool = new ArrayList<>(count);
        while (pool.size() < count) {
            C a = population.get(rng.nextInt(n));
            C b = population.get(rng.nextInt(n));
            int cmp = Dominance.compare(a, b);
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
        List<List<C>> fronts = NondominatedSorting.sort(union);

        List<C> next = new ArrayList<>(size);
        List<C> considered = new ArrayList<>();
        List<C> last = List.of();
        for (List<C> front : fronts) {
            considered.addAll(front);
            if (next.size() + front.size() <= size) {
                next.addAll(front);
                if (next.size() == size)
                    return next;
            } else {
                last = front;
                break;
            }
        }
        if (last.isEmpty())
            return next;

        int m = union.get(0).objectiveCount();
        List<double[]> refs = references(m, size);
        double[][] normalized = normalize(considered, fronts.get(0), m);

        int[] nicheCount = new int[refs.size()];
        int[] association = new int[considered.size()];
        double[] distance = new double[considered.size()];
        for (int i = 0; i < considered.size(); i++) {
            associate(normalized[i], refs, association, distance, i);
            if (i < next.size())
                nicheCount[association[i]]++;
        }

        // candidates of the last front, by index into "considered"
        List<Integer> candidates = new ArrayList<>();
        for (int i = next.size(); i < considered.size(); i++)
            candidates.add(i);
        boolean[] excluded = new boolean[refs.size()];
        RandomGenerator rng = ctx.rng();
        while (next.size() < size) {
            int minCount = Integer.MAX_VALUE;
            List<Integer> leastCrowded = new ArrayList<>();
            for (int j = 0; j < refs.size(); j++) {
                if (excluded[j])
                    continue;
                if (nicheCount[j] < minCount) {
                    minCount = nicheCount[j];
                    leastCrowded.clear();
                }
                if (nicheCount[j] == minCount)
                    leastCrowded.add(j);
            }
            int j = leastCrowded.get(rng.nextInt(leastCrowded.size()));
            List<Integer> members = new ArrayList<>();
            for (int c : candidates)
                if (association[c] == j)
                    members.add(c);
            if (members.isEmpty()) {
                excluded[j] = true;
                continue;
            }
            int pick;
            if (nicheCount[j] == 0) {
                pick = members.get(0);
                for (int c : members)
                    if (distance[c] < distance[pick])
                        pick = c;
            } else {
                pick = members.get(rng.nextInt(members.size()));
            }
            next.add(considered.get(pick));
            candidates.remove(Integer.valueOf(pick));
            nicheCount[j]++;
        }
        return next;
    }

    private List<double[]> references(int m, int populationSize) {
        if (references == null || references.get(0).length != m) {
            int p = divisions;
            if (p <= 0) {
                p = 1;
                while (binomial(p + 1 + m - 1, m - 1) <= populationSize)
                    p++;
            }
            references = Simplex.uniform(m, p);
        }
        return references;
    }

    private static long binomial(int n, int k) {
        long result = 1;
        for (int i = 1; i <= k; i++)
            result = result * (n - k + i) / i;
        return result;
    }

    /** Translated and scaled objectives of {@code set}; extreme points and nadir come from the first front. */
    private static double[][] normalize(List<? extends Chromosome> set, List<? extends Chromosome> firstFront, int m) {
        double[] ideal = new double[m];
        Arrays.fill(ideal, Double.POSITIVE_INFINITY);
        for (Chromosome c : set)
            for (int o = 0; o < m; o++)
                ideal[o] = Math.min(ideal[o], c.objective(o));

        double[][] extremes = new double[m][];
        for (int axis = 0; axis < m; axis++) {
            double best = Double.POSITIVE_INFINITY;
            for (Chromosome c : set) {
                double asf = 0.0;
                for (int o = 0; o < m; o++)
                    asf = Math.max(asf, (c.objective(o) - ideal[o]) / (o == axis ? 1.0 : 1e-6));
                if (asf < best) {
                    best = asf;
                    extremes[axis] = translated(c, ideal);
                }
            }
        }
        double[] intercepts = intercepts(extremes);
        if (intercepts == null) {
            intercepts = new double[m];
            for (Chromosome c : firstFront)
                for (int o = 0; o < m; o++)
                    intercepts[o] = Math.max(intercepts[o], c.objective(o) - ideal[o]);
        }
        for (int o = 0; o < m; o++)
            if (!(intercepts[o] > 1e-10))
                intercepts[o] = 1e-10;

        double[][] normalized = new double[set.size()][m];
        for (int i = 0; i < set.size(); i++)
            for (int o = 0; o < m; o++)
                normalized[i][o] = (set.get(i).objective(o) - ideal[o]) / intercepts[o];
        return normalized;
    }

    private static double[] translated(Chromosome c, double[] ideal) {
        double[] t = new double[ideal.length];
        for (int o = 0; o < ideal.length; o++)
            t[o] = c.objective(o) - ideal[o];
        return t;
    }

    /** Intercepts of the hyperplane through the extreme points, or null if it is degenerate. */
    private static double[] intercepts(double[][] extremes) {
        int m = extremes.length;
        double[][] a = new double[m][m + 1];
        for (int i = 0; i < m; i++) {
            System.arraycopy(extremes[i], 0, a[i], 0, m);
            a[i][m] = 1.0;
        }
        for (int col = 0; col < m; col++) { // Gaussian elimination with partial pivoting
            int pivot = col;
            for (int r = col + 1; r < m; r++)
                if (Math.abs(a[r][col]) > Math.abs(a[pivot][col]))
                    pivot = r;
            if (Math.abs(a[pivot][col]) < 1e-12)
                return null;
            double[] tmp = a[col];
            a[col] = a[pivot];
            a[pivot] = tmp;
            for (int r = 0; r < m; r++) {
                if (r == col)
                    continue;
                double factor = a[r][col] / a[col][col];
                for (int c = col; c <= m; c++)
                    a[r][c] -= factor * a[col][c];
            }
        }
        double[] intercepts = new double[m];
        for (int i = 0; i < m; i++) {
            double coefficient = a[i][m] / a[i][i];
            double intercept = 1.0 / coefficient;
            if (!(coefficient > 0) || !Double.isFinite(intercept) || intercept < 1e-6)
                return null;
            intercepts[i] = intercept;
        }
        return intercepts;
    }

    private static void associate(double[] point, List<double[]> refs, int[] association, double[] distance, int i) {
        double best = Double.POSITIVE_INFINITY;
        int bestRef = 0;
        for (int j = 0; j < refs.size(); j++) {
            double[] w = refs.get(j);
            double dot = 0.0;
            double norm = 0.0;
            for (int o = 0; o < w.length; o++) {
                dot += w[o] * point[o];
                norm += w[o] * w[o];
            }
            double scale = dot / norm;
            double d = 0.0;
            for (int o = 0; o < w.length; o++) {
                double diff = point[o] - scale * w[o];
                d += diff * diff;
            }
            if (d < best) {
                best = d;
                bestRef = j;
            }
        }
        association[i] = bestRef;
        distance[i] = Math.sqrt(best);
    }

    /** Visible for tests. */
    List<double[]> referencesFor(int objectives, int populationSize) {
        return references(objectives, populationSize);
    }
}
