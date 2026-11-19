package org.carlos.archipelagos.operator;

import java.util.random.RandomGenerator;

/** Operators for integer assignment genotypes (each gene independently in {@code [0, k)}). */
public final class IntOperators {

    private IntOperators() {
    }

    /** One-point crossover: swap tails starting at a random cut point. */
    public static void onePoint(int[] a, int[] b, RandomGenerator rng) {
        if (a.length < 2)
            return;
        int cut = 1 + rng.nextInt(a.length - 1);
        for (int j = cut; j < a.length; j++) {
            int tmp = a[j];
            a[j] = b[j];
            b[j] = tmp;
        }
    }

    /** Each gene with probability {@code rate} moves by +-1, wrapping around within {@code [0, k)} (2008 ALBP mutation). */
    public static void cyclicStep(int[] x, int k, double rate, RandomGenerator rng) {
        for (int i = 0; i < x.length; i++)
            if (rng.nextDouble() < rate)
                x[i] = Math.floorMod(x[i] + (rng.nextBoolean() ? 1 : -1), k);
    }
}
