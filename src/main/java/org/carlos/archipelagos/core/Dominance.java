package org.carlos.archipelagos.core;

/**
 * Pareto dominance for minimisation, extended with Deb's constrained-domination rules
 * (replaces the 2008 {@code ChromosomeRelation} + multiplicative penalty):
 * <ol>
 * <li>a feasible solution dominates an infeasible one,</li>
 * <li>of two infeasible solutions the one with smaller violation dominates,</li>
 * <li>two feasible solutions are compared by plain Pareto dominance.</li>
 * </ol>
 */
public final class Dominance {

    private Dominance() {
    }

    /** @return -1 if a dominates b, 1 if b dominates a, 0 if neither. */
    public static int compare(Chromosome a, Chromosome b) {
        boolean aFeasible = a.isFeasible();
        boolean bFeasible = b.isFeasible();
        if (aFeasible && !bFeasible)
            return -1;
        if (!aFeasible && bFeasible)
            return 1;
        if (!aFeasible)
            return Double.compare(a.violation(), b.violation());
        return pareto(a.objectives(), b.objectives());
    }

    public static boolean dominates(Chromosome a, Chromosome b) {
        return compare(a, b) < 0;
    }

    /** Plain Pareto comparison of two objective vectors (minimisation). */
    public static int pareto(double[] a, double[] b) {
        boolean aBetter = false;
        boolean bBetter = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i])
                aBetter = true;
            else if (a[i] > b[i])
                bBetter = true;
            if (aBetter && bBetter)
                return 0;
        }
        if (aBetter)
            return -1;
        if (bBetter)
            return 1;
        return 0;
    }

    /** Weak dominance ("covers" in SPEA terms): a is no worse than b in every objective. */
    public static boolean covers(Chromosome a, Chromosome b) {
        int cmp = compare(a, b);
        if (cmp != 0)
            return cmp < 0;
        if (!a.isFeasible() || !b.isFeasible())
            return a.violation() <= b.violation();
        double[] oa = a.objectives();
        double[] ob = b.objectives();
        for (int i = 0; i < oa.length; i++)
            if (oa[i] > ob[i])
                return false;
        return true;
    }
}
