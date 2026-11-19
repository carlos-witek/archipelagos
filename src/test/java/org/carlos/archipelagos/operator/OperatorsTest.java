package org.carlos.archipelagos.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.SplittableRandom;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class OperatorsTest {

    private final SplittableRandom rng = new SplittableRandom(7);

    @RepeatedTest(50)
    void pmxProducesPermutations() {
        int[] base = new int[12];
        for (int i = 0; i < base.length; i++)
            base[i] = i + 1;
        int[] a = PermutationOperators.shuffled(base, rng);
        int[] b = PermutationOperators.shuffled(base, rng);
        PermutationOperators.pmx(a, b, rng);
        PermutationOperators.inversion(a, 0.66, rng);
        PermutationOperators.swap(b, rng);
        assertThat(sorted(a)).isEqualTo(base);
        assertThat(sorted(b)).isEqualTo(base);
    }

    @RepeatedTest(50)
    void realOperatorsRespectBounds() {
        double[] lower = { -1, 0, 10 };
        double[] upper = { 1, 5, 11 };
        double[] a = { -1, 5, 10.5 };
        double[] b = { 1, 0, 11 };
        RealOperators.sbx(a, b, lower, upper, 15, rng);
        RealOperators.polynomialMutation(a, lower, upper, 20, 1.0, rng);
        RealOperators.averaging(a, b, lower, upper, rng);
        RealOperators.gaussianMutation(b, lower, upper, 0.5, 1.0, rng);
        for (int i = 0; i < 3; i++) {
            assertThat(a[i]).isBetween(lower[i], upper[i]);
            assertThat(b[i]).isBetween(lower[i], upper[i]);
        }
    }

    @Test
    void gaussianMutationMovesZeroGenes() {
        double[] x = { 0, 0, 0 };
        RealOperators.gaussianMutation(x, new double[] { -1, -1, -1 }, new double[] { 1, 1, 1 }, 0.1, 1.0, rng);
        assertThat(x).isNotEqualTo(new double[] { 0, 0, 0 });
    }

    @Test
    void cyclicStepWrapsWithinRange() {
        int[] x = { 0, 4, 2 };
        IntOperators.cyclicStep(x, 5, 1.0, rng);
        for (int v : x)
            assertThat(v).isBetween(0, 4);
    }

    private static int[] sorted(int[] x) {
        int[] copy = x.clone();
        Arrays.sort(copy);
        return copy;
    }
}
