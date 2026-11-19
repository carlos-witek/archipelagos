package org.carlos.archipelagos.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class MetricsTest {

    @Test
    void hypervolume2d() {
        List<double[]> front = List.of(new double[] { 1, 0 }, new double[] { 0, 1 });
        assertThat(Metrics.hypervolume(front, new double[] { 2, 2 })).isCloseTo(3.0, within(1e-12));
        // point outside the reference box contributes nothing
        assertThat(Metrics.hypervolume(List.of(new double[] { 3, 0 }), new double[] { 2, 2 })).isZero();
    }

    @Test
    void hypervolume3d() {
        assertThat(Metrics.hypervolume(List.of(new double[] { 0, 0, 0 }), new double[] { 1, 2, 3 }))
                .isCloseTo(6.0, within(1e-12));
        List<double[]> front = List.of(new double[] { 0, 1, 1 }, new double[] { 1, 0, 1 }, new double[] { 1, 1, 0 });
        // union of three 2x1x1 boxes inside [0,2]^3 overlapping pairwise in unit cubes: 3*2 - 3*1 + 1 = 4
        assertThat(Metrics.hypervolume(front, new double[] { 2, 2, 2 })).isCloseTo(4.0, within(1e-12));
    }

    @Test
    void distancesAreZeroOnTheReference() {
        List<double[]> ref = List.of(new double[] { 0, 1 }, new double[] { 0.5, 0.5 }, new double[] { 1, 0 });
        assertThat(Metrics.generationalDistance(ref, ref)).isZero();
        assertThat(Metrics.invertedGenerationalDistance(ref, ref)).isZero();
        assertThat(Metrics.invertedGenerationalDistancePlus(ref, ref)).isZero();
        assertThat(Metrics.maximumFrontError(ref, ref)).isZero();
    }

    @Test
    void generationalDistanceUsesSquaredDistances() {
        List<double[]> ref = List.of(new double[] { 0, 0 });
        List<double[]> front = List.of(new double[] { 3, 4 }, new double[] { 0, 0 });
        // sqrt(5^2 + 0^2) / 2 = 2.5  (2008 formula: sqrt(5 + 0) / 2)
        assertThat(Metrics.generationalDistance(front, ref)).isCloseTo(2.5, within(1e-12));
    }

    @Test
    void spacingOfEvenlySpacedFrontIsZero() {
        List<double[]> front = List.of(new double[] { 0, 3 }, new double[] { 1, 2 }, new double[] { 2, 1 },
                new double[] { 3, 0 });
        assertThat(Metrics.spacing(front)).isCloseTo(0.0, within(1e-12));
        assertThat(Metrics.maximumSpread(front)).isCloseTo(Math.sqrt(18), within(1e-12));
    }

    @Test
    void igdPlusIgnoresBeingBetterThanReference() {
        List<double[]> ref = List.of(new double[] { 1, 1 });
        assertThat(Metrics.invertedGenerationalDistancePlus(List.of(new double[] { 0, 0 }), ref)).isZero();
        assertThat(Metrics.invertedGenerationalDistance(List.of(new double[] { 0, 0 }), ref)).isPositive();
    }
}
