package org.carlos.archipelagos.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Front quality indicators on objective vectors (minimisation).
 * {@code front} is the approximation, {@code reference} the true front (or a dense sample).
 */
public final class Metrics {

    private Metrics() {
    }

    /**
     * Hypervolume dominated by {@code front} and bounded by {@code referencePoint}.
     * Exact sweep for 2 objectives, slicing (HSO) above that. Points not strictly better
     * than the reference point in every objective contribute nothing.
     */
    public static double hypervolume(List<double[]> front, double[] referencePoint) {
        List<double[]> points = new ArrayList<>();
        for (double[] p : front) {
            boolean inside = true;
            for (int k = 0; k < referencePoint.length; k++)
                if (!(p[k] < referencePoint[k])) {
                    inside = false;
                    break;
                }
            if (inside)
                points.add(p);
        }
        return hso(points, referencePoint, referencePoint.length);
    }

    private static double hso(List<double[]> points, double[] ref, int dims) {
        if (points.isEmpty())
            return 0.0;
        if (dims == 1) {
            double min = Double.POSITIVE_INFINITY;
            for (double[] p : points)
                min = Math.min(min, p[0]);
            return ref[0] - min;
        }
        if (dims == 2) {
            List<double[]> sorted = new ArrayList<>(points);
            sorted.sort(Comparator.comparingDouble(p -> p[0]));
            double volume = 0.0;
            double bestY = ref[1];
            for (double[] p : sorted)
                if (p[1] < bestY) {
                    volume += (ref[0] - p[0]) * (bestY - p[1]);
                    bestY = p[1];
                }
            return volume;
        }
        int last = dims - 1;
        List<double[]> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingDouble(p -> p[last]));
        double volume = 0.0;
        List<double[]> slice = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            slice.add(sorted.get(i));
            double top = i + 1 < sorted.size() ? sorted.get(i + 1)[last] : ref[last];
            double depth = top - sorted.get(i)[last];
            if (depth > 0)
                volume += depth * hso(slice, ref, last);
        }
        return volume;
    }

    /** Reference point = nadir of the reference front pushed out by 10% of its range (at least 1e-6). */
    public static double[] referencePointFor(List<double[]> reference) {
        int m = reference.get(0).length;
        double[] ideal = new double[m];
        double[] nadir = new double[m];
        java.util.Arrays.fill(ideal, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(nadir, Double.NEGATIVE_INFINITY);
        for (double[] p : reference)
            for (int k = 0; k < m; k++) {
                ideal[k] = Math.min(ideal[k], p[k]);
                nadir[k] = Math.max(nadir[k], p[k]);
            }
        double[] ref = new double[m];
        for (int k = 0; k < m; k++)
            ref[k] = nadir[k] + Math.max(1e-6, 0.1 * (nadir[k] - ideal[k]));
        return ref;
    }

    /**
     * Generational distance (Van Veldhuizen): sqrt(sum d_i^2) / n, d_i = distance from each
     * front point to the nearest reference point. (2008 summed d_i instead of d_i^2.)
     */
    public static double generationalDistance(List<double[]> front, List<double[]> reference) {
        double sum = 0.0;
        for (double[] p : front) {
            double d = nearest(p, reference);
            sum += d * d;
        }
        return Math.sqrt(sum) / front.size();
    }

    /** Inverted generational distance: mean distance from each reference point to the nearest front point. */
    public static double invertedGenerationalDistance(List<double[]> front, List<double[]> reference) {
        double sum = 0.0;
        for (double[] r : reference)
            sum += nearest(r, front);
        return sum / reference.size();
    }

    /** IGD+ (Ishibuchi 2015): only the amounts by which a front point is worse than the reference count. */
    public static double invertedGenerationalDistancePlus(List<double[]> front, List<double[]> reference) {
        double sum = 0.0;
        for (double[] r : reference) {
            double best = Double.POSITIVE_INFINITY;
            for (double[] a : front) {
                double d = 0.0;
                for (int k = 0; k < r.length; k++) {
                    double diff = Math.max(a[k] - r[k], 0.0);
                    d += diff * diff;
                }
                best = Math.min(best, Math.sqrt(d));
            }
            sum += best;
        }
        return sum / reference.size();
    }

    /**
     * Schott's spacing: standard deviation of each point's L1 distance to its nearest neighbour
     * <b>in the same front</b>. (2008 measured the distance to the reference front instead.)
     */
    public static double spacing(List<double[]> front) {
        int n = front.size();
        if (n < 2)
            return 0.0;
        double[] d = new double[n];
        double mean = 0.0;
        for (int i = 0; i < n; i++) {
            double best = Double.POSITIVE_INFINITY;
            for (int j = 0; j < n; j++) {
                if (i == j)
                    continue;
                double l1 = 0.0;
                for (int k = 0; k < front.get(i).length; k++)
                    l1 += Math.abs(front.get(i)[k] - front.get(j)[k]);
                best = Math.min(best, l1);
            }
            d[i] = best;
            mean += best;
        }
        mean /= n;
        double sum = 0.0;
        for (double di : d)
            sum += (mean - di) * (mean - di);
        return Math.sqrt(sum / (n - 1));
    }

    /** Zitzler's maximum spread: diagonal of the front's bounding box. */
    public static double maximumSpread(List<double[]> front) {
        int m = front.get(0).length;
        double sum = 0.0;
        for (int k = 0; k < m; k++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (double[] p : front) {
                min = Math.min(min, p[k]);
                max = Math.max(max, p[k]);
            }
            sum += (max - min) * (max - min);
        }
        return Math.sqrt(sum);
    }

    /** Maximum Pareto front error: the largest distance from a front point to the reference front. */
    public static double maximumFrontError(List<double[]> front, List<double[]> reference) {
        double worst = 0.0;
        for (double[] p : front)
            worst = Math.max(worst, nearest(p, reference));
        return worst;
    }

    private static double nearest(double[] p, List<double[]> set) {
        double best = Double.POSITIVE_INFINITY;
        for (double[] q : set) {
            double d = 0.0;
            for (int k = 0; k < p.length; k++) {
                double diff = p[k] - q[k];
                d += diff * diff;
            }
            best = Math.min(best, d);
        }
        return Math.sqrt(best);
    }
}
