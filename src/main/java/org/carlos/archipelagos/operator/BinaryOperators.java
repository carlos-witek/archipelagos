package org.carlos.archipelagos.operator;

import java.util.random.RandomGenerator;

public final class BinaryOperators {

    private BinaryOperators() {
    }

    public static void uniform(boolean[] a, boolean[] b, RandomGenerator rng) {
        for (int i = 0; i < a.length; i++)
            if (rng.nextBoolean()) {
                boolean tmp = a[i];
                a[i] = b[i];
                b[i] = tmp;
            }
    }

    public static void bitFlip(boolean[] x, double rate, RandomGenerator rng) {
        for (int i = 0; i < x.length; i++)
            if (rng.nextDouble() < rate)
                x[i] = !x[i];
    }
}
