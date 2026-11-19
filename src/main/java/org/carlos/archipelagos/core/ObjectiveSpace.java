package org.carlos.archipelagos.core;

import java.util.List;

/**
 * Per-objective bounds of a set of solutions and normalised distances.
 * Fixes the 2008 division by {@code max - min} when an objective is constant (NaN fitness).
 */
public final class ObjectiveSpace {

    private final double[] min;
    private final double[] max;
    private final double[] range;

    private ObjectiveSpace(double[] min, double[] max) {
        this.min = min;
        this.max = max;
        this.range = new double[min.length];
        for (int i = 0; i < min.length; i++) {
            double r = max[i] - min[i];
            range[i] = r > 0 && Double.isFinite(r) ? r : 1.0;
        }
    }

    public static ObjectiveSpace of(List<? extends Chromosome> solutions) {
        if (solutions.isEmpty())
            throw new IllegalArgumentException("empty population");
        int m = solutions.get(0).objectiveCount();
        double[] min = new double[m];
        double[] max = new double[m];
        java.util.Arrays.fill(min, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (Chromosome c : solutions)
            for (int k = 0; k < m; k++) {
                min[k] = Math.min(min[k], c.objective(k));
                max[k] = Math.max(max[k], c.objective(k));
            }
        return new ObjectiveSpace(min, max);
    }

    public int dimensions() {
        return min.length;
    }

    public double min(int k) {
        return min[k];
    }

    public double max(int k) {
        return max[k];
    }

    public double range(int k) {
        return range[k];
    }

    /** Objective k mapped to [0,1] (0 = best in the set). */
    public double normalized(Chromosome c, int k) {
        return (c.objective(k) - min[k]) / range[k];
    }

    /** Euclidean distance in normalised objective space. */
    public double distance(Chromosome a, Chromosome b) {
        double sum = 0.0;
        for (int k = 0; k < min.length; k++) {
            double d = (a.objective(k) - b.objective(k)) / range[k];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /** Goldberg's triangular sharing function. */
    public static double share(double distance, double sigmaShare) {
        return distance < sigmaShare ? 1.0 - distance / sigmaShare : 0.0;
    }
}
