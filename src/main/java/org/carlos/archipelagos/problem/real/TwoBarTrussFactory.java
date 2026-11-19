package org.carlos.archipelagos.problem.real;

import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.springframework.stereotype.Component;

/**
 * The thesis' two-bar truss: variables are the two bar cross-sections, the truss height and
 * the position of the load on the span. Objectives: total volume and maximal bar stress.
 * <p>
 * Fixes: the stress limit is a real constraint (violation = relative excess) instead of a penalty
 * that reached 1.0 and threw; evaluation is recomputed for every child, so no stale penalties;
 * variables are bounded (areas and height strictly positive, so no division by zero).
 */
@Component
public class TwoBarTrussFactory implements ProblemFactory {

    @Override
    public String name() {
        return "two-bar-truss";
    }

    @Override
    public String description() {
        return "Two-bar truss from the thesis: minimise volume and stress, stress limited.";
    }

    @Override
    public List<ParamSpec> params() {
        return RealProblem.operatorParams(List.of(
                ParamSpec.decimal("span", "span of the truss [m]", 5.0, 1e-3, 1e6),
                ParamSpec.bool("spanFixed", "load fixed at 4/5 of the span (ignores the 4th variable)", true),
                ParamSpec.decimal("force", "acting force [N]", 100.0, 0.0, 1e12),
                ParamSpec.decimal("stressLimit", "maximal allowed stress", 100000.0, 1e-9, 1e15),
                ParamSpec.decimal("maxRadius", "maximal bar radius [m]", 0.5, 1e-6, 1e3),
                ParamSpec.decimal("maxHeight", "maximal truss height [m]", 20.0, 1e-3, 1e6)));
    }

    @Override
    public Problem<?> create(Params p) {
        double span = p.getDouble("span");
        boolean spanFixed = p.getBoolean("spanFixed");
        double force = p.getDouble("force");
        double limit = p.getDouble("stressLimit");
        double maxArea = Math.PI * Math.pow(p.getDouble("maxRadius"), 2);
        double maxHeight = p.getDouble("maxHeight");
        double[] lower = { maxArea * 1e-6, maxArea * 1e-6, maxHeight * 1e-3, 0.0 };
        double[] upper = { maxArea, maxArea, maxHeight, span };
        return new RealProblem(name(), List.of("volume", "stress"), lower, upper, p) {

            private double d(double[] x) {
                return spanFixed ? 4 * span / 5 : x[3];
            }

            private double stress(double[] x) {
                double d = d(x);
                double y = x[2];
                double forceSpan = force / span;
                double s1 = forceSpan * d * Math.sqrt(Math.pow(span - d, 2) + y * y) / (x[0] * y);
                double s2 = forceSpan * (span - d) * Math.sqrt(d * d + y * y) / (x[1] * y);
                return Math.max(s1, s2);
            }

            @Override
            protected double[] compute(double[] x) {
                double d = d(x);
                double y = x[2];
                double volume = x[0] * Math.sqrt(Math.pow(span - d, 2) + y * y) + x[1] * Math.sqrt(d * d + y * y);
                return new double[] { volume, stress(x) };
            }

            @Override
            protected double violation(double[] x) {
                return Math.max(0.0, stress(x) / limit - 1.0);
            }
        };
    }
}
