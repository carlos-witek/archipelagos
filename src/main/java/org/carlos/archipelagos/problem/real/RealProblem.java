package org.carlos.archipelagos.problem.real;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.DoubleChromosome;
import org.carlos.archipelagos.operator.RealOperators;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.Variation;
import org.carlos.archipelagos.problem.VariationParams;

/**
 * Base for box-constrained real-valued problems: uniform initialisation inside the bounds and
 * either SBX + polynomial mutation (default) or the 2008 averaging crossover + bounded Gaussian
 * mutation.
 */
public abstract class RealProblem implements Problem<DoubleChromosome> {

    public static final String OPERATORS = "operators";

    private final String name;
    private final List<String> objectives;
    protected final double[] lower;
    protected final double[] upper;
    private final Variation<DoubleChromosome> variation;
    private volatile List<double[]> reference;

    protected RealProblem(String name, List<String> objectives, double[] lower, double[] upper, Params params) {
        if (lower.length != upper.length)
            throw new IllegalArgumentException("bounds length mismatch");
        for (int i = 0; i < lower.length; i++)
            if (!(lower[i] < upper[i]))
                throw new IllegalArgumentException("lower bound must be below upper bound for variable " + i);
        this.name = name;
        this.objectives = List.copyOf(objectives);
        this.lower = lower.clone();
        this.upper = upper.clone();
        this.variation = createVariation(params, lower.length);
    }

    public static List<ParamSpec> operatorParams(List<ParamSpec> specific) {
        List<ParamSpec> specs = new java.util.ArrayList<>(specific);
        specs.add(ParamSpec.choice(OPERATORS, "variation operators", "sbx", List.of("sbx", "averaging")));
        return VariationParams.with(specs);
    }

    protected static double[] filled(int n, double value) {
        double[] a = new double[n];
        Arrays.fill(a, value);
        return a;
    }

    private Variation<DoubleChromosome> createVariation(Params params, int n) {
        double crossoverRate = params.getDouble(VariationParams.CROSSOVER_RATE);
        double mutationRate = VariationParams.mutationRate(params, n);
        boolean sbx = "sbx".equals(params.getString(OPERATORS));
        return new Variation<>() {
            @Override
            public double crossoverRate() {
                return crossoverRate;
            }

            @Override
            public void crossover(DoubleChromosome a, DoubleChromosome b, RandomGenerator rng) {
                if (sbx)
                    RealOperators.sbx(a.genes, b.genes, lower, upper, 15.0, rng);
                else
                    RealOperators.averaging(a.genes, b.genes, lower, upper, rng);
            }

            @Override
            public void mutate(DoubleChromosome c, RandomGenerator rng) {
                if (sbx)
                    RealOperators.polynomialMutation(c.genes, lower, upper, 20.0, mutationRate, rng);
                else
                    RealOperators.gaussianMutation(c.genes, lower, upper, 0.1, mutationRate, rng);
            }
        };
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> objectives() {
        return objectives;
    }

    @Override
    public DoubleChromosome create(RandomGenerator rng) {
        DoubleChromosome c = new DoubleChromosome(lower.length);
        for (int i = 0; i < lower.length; i++)
            c.genes[i] = lower[i] + (upper[i] - lower[i]) * rng.nextDouble();
        return c;
    }

    @Override
    public void evaluate(DoubleChromosome c) {
        c.setEvaluation(compute(c.genes), violation(c.genes));
    }

    protected abstract double[] compute(double[] x);

    protected double violation(double[] x) {
        return 0.0;
    }

    @Override
    public Variation<DoubleChromosome> variation() {
        return variation;
    }

    @Override
    public Optional<List<double[]>> referenceFront() {
        List<double[]> ref = reference;
        if (ref == null) {
            ref = buildReferenceFront();
            reference = ref;
        }
        return Optional.ofNullable(ref);
    }

    /** Override to supply a reference front; {@code null} if unknown. */
    protected List<double[]> buildReferenceFront() {
        return null;
    }

    public double[] lower() {
        return lower.clone();
    }

    public double[] upper() {
        return upper.clone();
    }
}
