package org.carlos.archipelagos.problem.real;

import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.ReferenceFronts;
import org.springframework.stereotype.Component;

/**
 * The thesis' two-variable Schaffer variant: f1 = |x - (1,1)|^2, f2 = |x - (4,4)|^2.
 * Pareto set is the segment (1,1)-(4,4), so the front is f1 = 2s^2, f2 = 2(3-s)^2.
 */
@Component
public class SchafferFactory implements ProblemFactory {

    @Override
    public String name() {
        return "schaffer";
    }

    @Override
    public String description() {
        return "2-variable Schaffer variant from the thesis (distance to (1,1) and (4,4)).";
    }

    @Override
    public List<ParamSpec> params() {
        return RealProblem.operatorParams(List.of(ParamSpec.decimal("lower", "lower bound", -5.12),
                ParamSpec.decimal("upper", "upper bound", 5.12)));
    }

    @Override
    public Problem<?> create(Params p) {
        double lo = p.getDouble("lower");
        double hi = p.getDouble("upper");
        return new RealProblem(name(), List.of("f1", "f2"), RealProblem.filled(2, lo), RealProblem.filled(2, hi), p) {
            @Override
            protected double[] compute(double[] x) {
                double f1 = Math.pow(x[0] - 1, 2) + Math.pow(x[1] - 1, 2);
                double f2 = Math.pow(x[0] - 4, 2) + Math.pow(x[1] - 4, 2);
                return new double[] { f1, f2 };
            }

            @Override
            protected List<double[]> buildReferenceFront() {
                if (lo > 1 || hi < 4)
                    return null; // the Pareto set lies (partly) outside the box
                List<double[]> points = new java.util.ArrayList<>();
                for (int i = 0; i <= 1000; i++) {
                    double s = 3.0 * i / 1000;
                    points.add(new double[] { 2 * s * s, 2 * (3 - s) * (3 - s) });
                }
                return ReferenceFronts.nondominated(points);
            }
        };
    }
}
