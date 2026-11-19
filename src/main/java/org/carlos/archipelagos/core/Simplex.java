package org.carlos.archipelagos.core;

import java.util.ArrayList;
import java.util.List;

/** Das & Dennis evenly spaced points on the unit simplex (weights summing to 1). */
public final class Simplex {

    private Simplex() {
    }

    public static List<double[]> uniform(int dimensions, int divisions) {
        List<double[]> result = new ArrayList<>();
        fill(result, new double[dimensions], 0, divisions, divisions);
        return result;
    }

    private static void fill(List<double[]> out, double[] current, int index, int left, int divisions) {
        if (index == current.length - 1) {
            current[index] = (double) left / divisions;
            out.add(current.clone());
            return;
        }
        for (int i = 0; i <= left; i++) {
            current[index] = (double) i / divisions;
            fill(out, current, index + 1, left - i, divisions);
        }
    }
}
