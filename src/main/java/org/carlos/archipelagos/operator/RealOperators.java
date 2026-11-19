package org.carlos.archipelagos.operator;

import java.util.random.RandomGenerator;

/** Variation operators for real-valued genes inside box bounds. */
public final class RealOperators {

    private RealOperators() {
    }

    /** Deb & Agrawal's simulated binary crossover (bounded variant), per gene with probability 0.5. */
    public static void sbx(double[] a, double[] b, double[] lower, double[] upper, double eta, RandomGenerator rng) {
        for (int i = 0; i < a.length; i++) {
            if (rng.nextDouble() > 0.5 || Math.abs(a[i] - b[i]) < 1e-14)
                continue;
            double y1 = Math.min(a[i], b[i]);
            double y2 = Math.max(a[i], b[i]);
            double lb = lower[i];
            double ub = upper[i];
            double u = rng.nextDouble();

            double beta = 1.0 + 2.0 * (y1 - lb) / (y2 - y1);
            double alpha = 2.0 - Math.pow(beta, -(eta + 1.0));
            double c1 = 0.5 * ((y1 + y2) - betaQ(u, alpha, eta) * (y2 - y1));

            beta = 1.0 + 2.0 * (ub - y2) / (y2 - y1);
            alpha = 2.0 - Math.pow(beta, -(eta + 1.0));
            double c2 = 0.5 * ((y1 + y2) + betaQ(u, alpha, eta) * (y2 - y1));

            c1 = clamp(c1, lb, ub);
            c2 = clamp(c2, lb, ub);
            if (rng.nextBoolean()) {
                a[i] = c2;
                b[i] = c1;
            } else {
                a[i] = c1;
                b[i] = c2;
            }
        }
    }

    private static double betaQ(double u, double alpha, double eta) {
        if (u <= 1.0 / alpha)
            return Math.pow(u * alpha, 1.0 / (eta + 1.0));
        return Math.pow(1.0 / (2.0 - u * alpha), 1.0 / (eta + 1.0));
    }

    /** Deb's polynomial mutation, each gene with probability {@code rate}. */
    public static void polynomialMutation(double[] x, double[] lower, double[] upper, double eta, double rate,
            RandomGenerator rng) {
        for (int i = 0; i < x.length; i++) {
            if (rng.nextDouble() >= rate)
                continue;
            double lb = lower[i];
            double ub = upper[i];
            double range = ub - lb;
            if (range <= 0)
                continue;
            double d1 = (x[i] - lb) / range;
            double d2 = (ub - x[i]) / range;
            double u = rng.nextDouble();
            double power = 1.0 / (eta + 1.0);
            double dq;
            if (u < 0.5) {
                double val = 2.0 * u + (1.0 - 2.0 * u) * Math.pow(1.0 - d1, eta + 1.0);
                dq = Math.pow(val, power) - 1.0;
            } else {
                double val = 2.0 * (1.0 - u) + 2.0 * (u - 0.5) * Math.pow(1.0 - d2, eta + 1.0);
                dq = 1.0 - Math.pow(val, power);
            }
            x[i] = clamp(x[i] + dq * range, lb, ub);
        }
    }

    /**
     * The 2008 "averaging" crossover (both children move towards each other by a random
     * fraction of their distance), now clamped to the bounds.
     */
    public static void averaging(double[] a, double[] b, double[] lower, double[] upper, RandomGenerator rng) {
        for (int i = 0; i < a.length; i++) {
            double modifier = rng.nextDouble() * (a[i] - b[i]);
            a[i] = clamp(a[i] - modifier, lower[i], upper[i]);
            b[i] = clamp(b[i] + modifier, lower[i], upper[i]);
        }
    }

    /**
     * Gaussian mutation with sigma relative to the variable range. Replaces the 2008
     * {@code g += 0.1 * g * N(0,1)}, which could never move a gene that was exactly 0
     * and ignored the bounds.
     */
    public static void gaussianMutation(double[] x, double[] lower, double[] upper, double sigmaFraction, double rate,
            RandomGenerator rng) {
        for (int i = 0; i < x.length; i++)
            if (rng.nextDouble() < rate)
                x[i] = clamp(x[i] + rng.nextGaussian() * sigmaFraction * (upper[i] - lower[i]), lower[i], upper[i]);
    }

    public static double clamp(double value, double lower, double upper) {
        return value < lower ? lower : (value > upper ? upper : value);
    }
}
