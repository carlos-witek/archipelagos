package org.carlos.archipelagos.problem.albp;

import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.IntChromosome;
import org.carlos.archipelagos.operator.IntOperators;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.Variation;
import org.carlos.archipelagos.problem.VariationParams;
import org.springframework.stereotype.Component;

/**
 * Mixed-model assembly line balancing (thesis problem). Gene i = station of task i+1.
 * <ul>
 * <li>{@code standardDeviation}: for each station, the standard deviation of its load over the
 * product variants, averaged over stations. (2008 summed |x - mean| instead; the correct formula
 * was commented out.)</li>
 * <li>{@code imbalance}: sum over stations of (mean station load - mean load)^2 / mean load.</li>
 * <li>Precedence violations (a successor placed on an earlier station) are a constraint
 * (violation = number of violated edges) instead of the 2008 quadratic penalty.</li>
 * </ul>
 * Single-variant data sets (IN2 files) always have standardDeviation = 0.
 * (2008's {@code ALBPProblem} constructor cast its fitness to {@code CVRPFitness} and could not
 * be instantiated at all.)
 */
@Component
public class AlbpFactory implements ProblemFactory {

    public static final List<String> DATA_SETS = List.of("problem01", "problem02", "problem03", "problem04", "jackson",
            "vilarinho", "BOWMAN.IN2", "JAESCHKE.IN2", "MANSOOR.IN2");

    @Override
    public String name() {
        return "albp";
    }

    @Override
    public String description() {
        return "Mixed-model assembly line balancing (thesis data sets + Scholl IN2 instances).";
    }

    @Override
    public List<ParamSpec> params() {
        return VariationParams.with(List.of(ParamSpec.choice("dataSet", "data set", "problem01", DATA_SETS),
                ParamSpec.integer("stations", "number of stations", 10, 2, 1000)));
    }

    @Override
    public Problem<?> create(Params p) {
        return new AlbpProblem(AlbpData.load(p.getString("dataSet")), p.getInt("stations"),
                p.getDouble(VariationParams.CROSSOVER_RATE), p.getDouble(VariationParams.MUTATION_RATE));
    }

    public static final class AlbpProblem implements Problem<IntChromosome> {

        private final AlbpData data;
        private final int stations;
        private final Variation<IntChromosome> variation;

        public AlbpProblem(AlbpData data, int stations, double crossoverRate, double mutationRate) {
            this.data = data;
            this.stations = stations;
            double rate = mutationRate > 0 ? mutationRate : 1.0 / data.tasks();
            this.variation = new Variation<>() {
                @Override
                public double crossoverRate() {
                    return crossoverRate;
                }

                @Override
                public void crossover(IntChromosome a, IntChromosome b, RandomGenerator rng) {
                    IntOperators.onePoint(a.genes, b.genes, rng);
                }

                @Override
                public void mutate(IntChromosome c, RandomGenerator rng) {
                    IntOperators.cyclicStep(c.genes, stations, rate, rng);
                }
            };
        }

        @Override
        public String name() {
            return "albp/" + data.name();
        }

        @Override
        public List<String> objectives() {
            return List.of("standardDeviation", "imbalance");
        }

        @Override
        public IntChromosome create(RandomGenerator rng) {
            IntChromosome c = new IntChromosome(data.tasks());
            for (int i = 0; i < c.genes.length; i++)
                c.genes[i] = rng.nextInt(stations);
            return c;
        }

        @Override
        public void evaluate(IntChromosome c) {
            int v = data.variants();
            double[][] load = new double[stations][v];
            for (int task = 0; task < data.tasks(); task++)
                for (int variant = 0; variant < v; variant++)
                    load[c.genes[task]][variant] += data.times()[task][variant];

            double[] mean = new double[stations];
            double overallMean = 0.0;
            double deviation = 0.0;
            for (int s = 0; s < stations; s++) {
                for (int variant = 0; variant < v; variant++)
                    mean[s] += load[s][variant];
                mean[s] /= v;
                overallMean += mean[s];
                double variance = 0.0;
                for (int variant = 0; variant < v; variant++)
                    variance += Math.pow(load[s][variant] - mean[s], 2);
                deviation += Math.sqrt(variance / v);
            }
            overallMean /= stations;
            deviation /= stations;

            double imbalance = 0.0;
            for (int s = 0; s < stations; s++)
                imbalance += Math.pow(mean[s] - overallMean, 2);
            imbalance = overallMean > 0 ? imbalance / overallMean : 0.0;

            int violations = 0;
            for (int[] edge : data.edges())
                if (c.genes[edge[1] - 1] < c.genes[edge[0] - 1])
                    violations++;
            c.setEvaluation(new double[] { deviation, imbalance }, violations);
        }

        @Override
        public Variation<IntChromosome> variation() {
            return variation;
        }
    }
}
