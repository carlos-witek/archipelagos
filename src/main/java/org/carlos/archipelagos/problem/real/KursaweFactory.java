package org.carlos.archipelagos.problem.real;

import java.util.List;

import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.ReferenceFronts;
import org.springframework.stereotype.Component;

/**
 * Kursawe (1990), 3 variables in [-5, 5]. The reference front is the 2008 brute-force grid result
 * (step 0.005) from {@code archipelagos-client/results/kursawe/front-complete.xml}, converted to CSV.
 */
@Component
public class KursaweFactory implements ProblemFactory {

    @Override
    public String name() {
        return "kursawe";
    }

    @Override
    public String description() {
        return "Kursawe: 3 variables in [-5,5], disconnected front. Reference = 2008 brute-force grid.";
    }

    @Override
    public List<ParamSpec> params() {
        return RealProblem.operatorParams(List.<ParamSpec>of());
    }

    @Override
    public Problem<?> create(Params p) {
        return new RealProblem(name(), List.of("func1", "func2"), RealProblem.filled(3, -5), RealProblem.filled(3, 5), p) {
            @Override
            protected double[] compute(double[] x) {
                double f1 = 0.0;
                for (int i = 0; i < 2; i++)
                    f1 += -10 * Math.exp(-0.2 * Math.sqrt(x[i] * x[i] + x[i + 1] * x[i + 1]));
                double f2 = 0.0;
                for (double xi : x)
                    f2 += Math.pow(Math.abs(xi), 0.8) + 5 * Math.sin(xi * xi * xi);
                return new double[] { f1, f2 };
            }

            @Override
            protected List<double[]> buildReferenceFront() {
                return ReferenceFronts.csv("/data/reference/kursawe.csv");
            }
        };
    }
}
