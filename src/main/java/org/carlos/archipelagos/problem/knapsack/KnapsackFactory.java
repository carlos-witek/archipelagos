package org.carlos.archipelagos.problem.knapsack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.BinaryChromosome;
import org.carlos.archipelagos.operator.BinaryOperators;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.Variation;
import org.carlos.archipelagos.problem.VariationParams;
import org.springframework.stereotype.Component;

/**
 * 0/1 knapsack on the thesis data files (1000 items, "value weight" per line; line 3 is the
 * capacity = half of the total weight).
 * <p>
 * <b>Reformulated.</b> The 2008 version never ran (objective names did not contain the ':' it split
 * on, and a full last knapsack indexed past the list) and used a permutation decoder over two
 * knapsacks whose capacities it read from a line that actually holds item counts. Here it is the
 * standard bi-objective formulation: a bit string, maximise total value (reported as
 * {@code negValue} because everything is minimised) and minimise total weight, subject to
 * weight &lt;= capacity.
 */
@Component
public class KnapsackFactory implements ProblemFactory {

    @Override
    public String name() {
        return "knapsack";
    }

    @Override
    public String description() {
        return "Bi-objective 0/1 knapsack (max value, min weight, capacity constraint) on the thesis data.";
    }

    @Override
    public List<ParamSpec> params() {
        return VariationParams.with(List.of(ParamSpec.choice("dataSet", "data set", "K02_I1000_1",
                List.of("K02_I1000_1", "K02_I1000_2", "K02_I1000_3", "K02_I1000_4")),
                ParamSpec.integer("items", "use only the first N items (0 = all)", 0, 0, 1000000)));
    }

    @Override
    public Problem<?> create(Params p) {
        Instance instance = load(p.getString("dataSet"), p.getInt("items"));
        double crossoverRate = p.getDouble(VariationParams.CROSSOVER_RATE);
        double mutationRate = VariationParams.mutationRate(p, instance.values.length);
        return new Problem<BinaryChromosome>() {
            private final Variation<BinaryChromosome> variation = new Variation<>() {
                @Override
                public double crossoverRate() {
                    return crossoverRate;
                }

                @Override
                public void crossover(BinaryChromosome a, BinaryChromosome b, RandomGenerator rng) {
                    BinaryOperators.uniform(a.genes, b.genes, rng);
                }

                @Override
                public void mutate(BinaryChromosome c, RandomGenerator rng) {
                    BinaryOperators.bitFlip(c.genes, mutationRate, rng);
                }
            };

            @Override
            public String name() {
                return "knapsack/" + p.getString("dataSet");
            }

            @Override
            public List<String> objectives() {
                return List.of("negValue", "weight");
            }

            @Override
            public BinaryChromosome create(RandomGenerator rng) {
                BinaryChromosome c = new BinaryChromosome(instance.values.length);
                // sparse start: each item packed with probability 1/4, so initial solutions are mostly feasible
                for (int i = 0; i < c.genes.length; i++)
                    c.genes[i] = rng.nextInt(4) == 0;
                return c;
            }

            @Override
            public void evaluate(BinaryChromosome c) {
                long value = 0;
                long weight = 0;
                for (int i = 0; i < c.genes.length; i++)
                    if (c.genes[i]) {
                        value += instance.values[i];
                        weight += instance.weights[i];
                    }
                double violation = Math.max(0.0, (double) (weight - instance.capacity) / instance.capacity);
                c.setEvaluation(new double[] { -value, weight }, violation);
            }

            @Override
            public Variation<BinaryChromosome> variation() {
                return variation;
            }
        };
    }

    record Instance(int[] values, int[] weights, long capacity) {
    }

    static Instance load(String dataSet, int limit) {
        String resource = "/data/knapsack/" + dataSet + ".txt";
        InputStream in = KnapsackFactory.class.getResourceAsStream(resource);
        if (in == null)
            throw new IllegalArgumentException("Unknown knapsack data set " + dataSet);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII))) {
            int items = Integer.parseInt(reader.readLine().trim());
            reader.readLine(); // number of knapsacks (2), unused
            long capacity = Long.parseLong(reader.readLine().trim());
            reader.readLine(); // "501  499": item counts, not capacities
            int n = limit > 0 ? Math.min(limit, items) : items;
            int[] values = new int[n];
            int[] weights = new int[n];
            for (int i = 0; i < n; i++) {
                String[] parts = reader.readLine().trim().split("\\s+");
                values[i] = Integer.parseInt(parts[0]);
                weights[i] = Integer.parseInt(parts[1]);
            }
            if (n < items) {
                long total = 0;
                for (int w : weights)
                    total += w;
                capacity = total / 2;
            }
            return new Instance(values, weights, Math.max(1, capacity));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
