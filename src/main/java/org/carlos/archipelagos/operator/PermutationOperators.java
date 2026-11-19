package org.carlos.archipelagos.operator;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * Permutation operators. The 2008 {@code RecombinationPerm} crashed immediately
 * ({@code (int) maxCrossoverLength * length} is {@code 0 * length}, then {@code nextInt(0)});
 * this is a standard partially-mapped crossover plus swap/inversion mutation.
 */
public final class PermutationOperators {

    private PermutationOperators() {
    }

    /** Random permutation of {@code values} (Fisher-Yates). */
    public static int[] shuffled(int[] values, RandomGenerator rng) {
        int[] result = values.clone();
        for (int i = result.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = result[i];
            result[i] = result[j];
            result[j] = tmp;
        }
        return result;
    }

    /** Partially-mapped crossover (Goldberg & Lingle), produces two children in place. */
    public static void pmx(int[] a, int[] b, RandomGenerator rng) {
        int n = a.length;
        if (n < 2)
            return;
        int i = rng.nextInt(n);
        int j = rng.nextInt(n);
        int from = Math.min(i, j);
        int to = Math.max(i, j);
        int[] childA = pmxChild(a, b, from, to);
        int[] childB = pmxChild(b, a, from, to);
        System.arraycopy(childA, 0, a, 0, n);
        System.arraycopy(childB, 0, b, 0, n);
    }

    /** Child takes {@code segmentDonor[from..to]} and the rest from {@code other}, repaired through the mapping. */
    private static int[] pmxChild(int[] other, int[] segmentDonor, int from, int to) {
        int n = other.length;
        int[] child = new int[n];
        Arrays.fill(child, Integer.MIN_VALUE);
        java.util.Map<Integer, Integer> mapping = new java.util.HashMap<>();
        java.util.Set<Integer> inSegment = new java.util.HashSet<>();
        for (int k = from; k <= to; k++) {
            child[k] = segmentDonor[k];
            inSegment.add(segmentDonor[k]);
            mapping.put(segmentDonor[k], other[k]);
        }
        for (int k = 0; k < n; k++) {
            if (k >= from && k <= to)
                continue;
            int value = other[k];
            while (inSegment.contains(value))
                value = mapping.get(value);
            child[k] = value;
        }
        return child;
    }

    public static void swap(int[] x, RandomGenerator rng) {
        if (x.length < 2)
            return;
        int i = rng.nextInt(x.length);
        int j = rng.nextInt(x.length - 1);
        if (j >= i)
            j++;
        int tmp = x[i];
        x[i] = x[j];
        x[j] = tmp;
    }

    /** Reverses a random segment of at most {@code maxFraction * n} genes. */
    public static void inversion(int[] x, double maxFraction, RandomGenerator rng) {
        int n = x.length;
        if (n < 2)
            return;
        int maxLength = Math.max(2, Math.min(n, (int) Math.round(maxFraction * n)));
        int length = 2 + rng.nextInt(maxLength - 1);
        int start = rng.nextInt(n - length + 1);
        for (int l = start, r = start + length - 1; l < r; l++, r--) {
            int tmp = x[l];
            x[l] = x[r];
            x[r] = tmp;
        }
    }
}
