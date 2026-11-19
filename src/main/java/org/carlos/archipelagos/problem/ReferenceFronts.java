package org.carlos.archipelagos.problem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import org.carlos.archipelagos.core.Dominance;

/** Reference Pareto fronts: analytical samples and bundled CSV data. */
public final class ReferenceFronts {

    private ReferenceFronts() {
    }

    /** Samples {@code f2 = shape(f1)} for f1 in [from, to] and keeps the non-dominated points. */
    public static List<double[]> curve(double from, double to, int points, DoubleUnaryOperator shape) {
        List<double[]> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double f1 = from + (to - from) * i / (points - 1);
            result.add(new double[] { f1, shape.applyAsDouble(f1) });
        }
        return nondominated(result);
    }

    public static List<double[]> nondominated(List<double[]> points) {
        List<double[]> result = new ArrayList<>();
        for (double[] p : points) {
            boolean dominated = false;
            for (double[] q : points)
                if (q != p && Dominance.pareto(q, p) < 0) {
                    dominated = true;
                    break;
                }
            if (!dominated)
                result.add(p);
        }
        return result;
    }

    /** Loads a classpath CSV with a header line and one point per row. */
    public static List<double[]> csv(String resource) {
        InputStream in = ReferenceFronts.class.getResourceAsStream(resource);
        if (in == null)
            throw new IllegalStateException("Missing resource " + resource);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<double[]> points = new ArrayList<>();
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split(",");
                double[] point = new double[parts.length];
                for (int i = 0; i < parts.length; i++)
                    point[i] = Double.parseDouble(parts[i].trim());
                points.add(point);
            }
            return nondominated(points);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
