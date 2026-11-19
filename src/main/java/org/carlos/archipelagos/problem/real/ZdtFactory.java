package org.carlos.archipelagos.problem.real;

import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.ReferenceFronts;
import org.springframework.stereotype.Component;

/** ZDT1-4 and ZDT6 (Zitzler, Deb & Thiele 2000) with analytical fronts. Not in the thesis. */
@Component
public class ZdtFactory implements ProblemFactory {

    @Override
    public String name() {
        return "zdt";
    }

    @Override
    public String description() {
        return "ZDT benchmark suite (variants 1, 2, 3, 4, 6), analytical Pareto fronts.";
    }

    @Override
    public List<ParamSpec> params() {
        return RealProblem.operatorParams(List.of(
                ParamSpec.choice("variant", "ZDT variant", "1", List.of("1", "2", "3", "4", "6")),
                ParamSpec.integer("variables", "number of variables (0 = standard: 30, or 10 for ZDT4/6)", 0, 0, 1000)));
    }

    @Override
    public Problem<?> create(Params p) {
        int variant = Integer.parseInt(p.getString("variant"));
        int n = p.getInt("variables");
        if (n == 0)
            n = variant == 4 || variant == 6 ? 10 : 30;
        if (n < 2)
            throw new IllegalArgumentException("ZDT needs at least 2 variables");
        double[] lower = RealProblem.filled(n, variant == 4 ? -5 : 0);
        double[] upper = RealProblem.filled(n, variant == 4 ? 5 : 1);
        lower[0] = 0;
        upper[0] = 1;
        final int size = n;
        return new RealProblem("zdt" + variant, List.of("f1", "f2"), lower, upper, p) {
            @Override
            protected double[] compute(double[] x) {
                double sum = 0.0;
                for (int i = 1; i < size; i++)
                    sum += variant == 4 ? x[i] * x[i] - 10 * Math.cos(4 * Math.PI * x[i]) : x[i];
                double f1;
                double g;
                if (variant == 6) {
                    f1 = 1 - Math.exp(-4 * x[0]) * Math.pow(Math.sin(6 * Math.PI * x[0]), 6);
                    g = 1 + 9 * Math.pow(sum / (size - 1), 0.25);
                } else {
                    f1 = x[0];
                    g = variant == 4 ? 1 + 10 * (size - 1) + sum : 1 + 9 * sum / (size - 1);
                }
                double h = switch (variant) {
                case 2, 6 -> 1 - Math.pow(f1 / g, 2);
                case 3 -> 1 - Math.sqrt(f1 / g) - (f1 / g) * Math.sin(10 * Math.PI * f1);
                default -> 1 - Math.sqrt(f1 / g);
                };
                return new double[] { f1, g * h };
            }

            @Override
            protected List<double[]> buildReferenceFront() {
                return switch (variant) {
                case 2 -> ReferenceFronts.curve(0, 1, 1000, f -> 1 - f * f);
                case 3 -> ReferenceFronts.curve(0, 0.852, 5000, f -> 1 - Math.sqrt(f) - f * Math.sin(10 * Math.PI * f));
                case 6 -> ReferenceFronts.curve(0.2807753191, 1, 1000, f -> 1 - f * f);
                default -> ReferenceFronts.curve(0, 1, 1000, f -> 1 - Math.sqrt(f));
                };
            }
        };
    }
}
