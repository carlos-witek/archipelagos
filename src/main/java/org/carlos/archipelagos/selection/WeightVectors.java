package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.List;

import org.carlos.archipelagos.core.Simplex;

/** Parses "w1,w2;w1,w2;..." weight vectors, or generates evenly spaced ones when empty. */
final class WeightVectors {

    private WeightVectors() {
    }

    static List<double[]> parseOrGenerate(String data, int objectives) {
        if (data == null || data.isBlank())
            return Simplex.uniform(objectives, objectives == 2 ? 4 : 3);
        List<double[]> vectors = new ArrayList<>();
        for (String part : data.split(";")) {
            String[] values = part.trim().split(",");
            if (values.length != objectives)
                throw new IllegalArgumentException(
                        "Weight vector '" + part + "' has " + values.length + " components, problem has " + objectives
                                + " objectives");
            double[] vector = new double[objectives];
            double sum = 0.0;
            for (int i = 0; i < objectives; i++) {
                vector[i] = Double.parseDouble(values[i].trim());
                if (vector[i] < 0)
                    throw new IllegalArgumentException("Weights must be non-negative: " + part);
                sum += vector[i];
            }
            if (sum <= 0)
                throw new IllegalArgumentException("Weight vector sums to zero: " + part);
            for (int i = 0; i < objectives; i++)
                vector[i] /= sum;
            vectors.add(vector);
        }
        return vectors;
    }
}
