package org.carlos.archipelagos.problem.albp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A (mixed-model) assembly line balancing instance: tasks 1..n, precedence edges and the
 * duration of every task in every product variant.
 * <p>
 * Two formats are read:
 * <ul>
 * <li>the thesis format: {@code graph.properties} ("task,successor,...") and
 * {@code variants.properties} ("task,time1,time2,...")</li>
 * <li>Scholl's {@code .IN2} SALBP format (n, n durations, "a,b" edges, "-1,-1"), giving one variant.
 * The 2008 Bowman files mixed both formats and could not be loaded; they are now stored as IN2.</li>
 * </ul>
 */
public record AlbpData(String name, int tasks, int variants, List<int[]> edges, double[][] times) {

    public static AlbpData load(String dataSet) {
        String base = "/data/albp/";
        if (dataSet.endsWith(".IN2"))
            return fromIn2(dataSet, read(base + dataSet));
        return fromProperties(dataSet, read(base + dataSet + "/graph.properties"),
                read(base + dataSet + "/variants.properties"));
    }

    static AlbpData fromProperties(String name, List<String> graph, List<String> variantLines) {
        Map<Integer, double[]> times = new TreeMap<>();
        int variants = -1;
        for (String line : variantLines) {
            String[] values = line.split(",");
            double[] t = new double[values.length - 1];
            for (int i = 1; i < values.length; i++)
                t[i - 1] = Double.parseDouble(values[i].trim());
            if (variants == -1)
                variants = t.length;
            else if (variants != t.length)
                throw new IllegalStateException(name + ": task " + values[0] + " has " + t.length + " variants, expected " + variants);
            times.put(Integer.parseInt(values[0].trim()), t);
        }
        int n = times.size();
        List<int[]> edges = new ArrayList<>();
        for (String line : graph) {
            String[] values = line.split(",");
            int source = Integer.parseInt(values[0].trim());
            for (int i = 1; i < values.length; i++)
                edges.add(new int[] { source, Integer.parseInt(values[i].trim()) });
        }
        return build(name, n, Math.max(variants, 1), edges, times);
    }

    static AlbpData fromIn2(String name, List<String> lines) {
        int n = Integer.parseInt(lines.get(0).trim());
        Map<Integer, double[]> times = new TreeMap<>();
        for (int i = 1; i <= n; i++)
            times.put(i, new double[] { Double.parseDouble(lines.get(i).trim()) });
        List<int[]> edges = new ArrayList<>();
        for (int i = n + 1; i < lines.size(); i++) {
            String[] pair = lines.get(i).split(",");
            int a = Integer.parseInt(pair[0].trim());
            int b = Integer.parseInt(pair[1].trim());
            if (a == -1 && b == -1)
                break;
            edges.add(new int[] { a, b });
        }
        return build(name, n, 1, edges, times);
    }

    private static AlbpData build(String name, int n, int variants, List<int[]> edges, Map<Integer, double[]> times) {
        double[][] matrix = new double[n][];
        for (int task = 1; task <= n; task++) {
            double[] t = times.get(task);
            if (t == null)
                throw new IllegalStateException(name + ": tasks must be numbered 1.." + n + ", missing " + task);
            matrix[task - 1] = t;
        }
        for (int[] edge : edges)
            if (edge[0] < 1 || edge[0] > n || edge[1] < 1 || edge[1] > n)
                throw new IllegalStateException(name + ": edge " + edge[0] + "->" + edge[1] + " refers to an unknown task");
        return new AlbpData(name, n, variants, List.copyOf(edges), matrix);
    }

    private static List<String> read(String resource) {
        InputStream in = AlbpData.class.getResourceAsStream(resource);
        if (in == null)
            throw new IllegalArgumentException("Unknown ALBP data set resource " + resource);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty() && !line.startsWith("#"))
                    lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
