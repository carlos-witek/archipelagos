package org.carlos.archipelagos.problem.real;

import java.util.ArrayList;
import java.util.List;

import org.carlos.archipelagos.core.Simplex;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.springframework.stereotype.Component;

/** DTLZ1 and DTLZ2 (Deb et al. 2002), scalable in the number of objectives. Not in the thesis. */
@Component
public class DtlzFactory implements ProblemFactory {

    @Override
    public String name() {
        return "dtlz";
    }

    @Override
    public String description() {
        return "DTLZ1 (linear front) and DTLZ2 (spherical front), M objectives, analytical fronts.";
    }

    @Override
    public List<ParamSpec> params() {
        return RealProblem.operatorParams(List.of(
                ParamSpec.choice("variant", "DTLZ variant", "2", List.of("1", "2")),
                ParamSpec.integer("objectives", "number of objectives", 3, 2, 10)));
    }

    @Override
    public Problem<?> create(Params p) {
        int variant = Integer.parseInt(p.getString("variant"));
        int m = p.getInt("objectives");
        int k = variant == 1 ? 5 : 10;
        int n = m + k - 1;
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= m; i++)
            names.add("f" + i);
        return new RealProblem("dtlz" + variant, names, RealProblem.filled(n, 0), RealProblem.filled(n, 1), p) {
            @Override
            protected double[] compute(double[] x) {
                double g = 0.0;
                for (int i = m - 1; i < n; i++) {
                    double d = x[i] - 0.5;
                    g += variant == 1 ? d * d - Math.cos(20 * Math.PI * d) : d * d;
                }
                if (variant == 1)
                    g = 100 * (k + g);
                double[] f = new double[m];
                for (int i = 0; i < m; i++) {
                    double value = variant == 1 ? 0.5 * (1 + g) : 1 + g;
                    for (int j = 0; j < m - 1 - i; j++)
                        value *= variant == 1 ? x[j] : Math.cos(x[j] * Math.PI / 2);
                    if (i > 0) {
                        double last = x[m - 1 - i];
                        value *= variant == 1 ? 1 - last : Math.sin(last * Math.PI / 2);
                    }
                    f[i] = value;
                }
                return f;
            }

            @Override
            protected List<double[]> buildReferenceFront() {
                List<double[]> points = Simplex.uniform(m, m == 2 ? 500 : m == 3 ? 30 : 8);
                for (double[] point : points) {
                    if (variant == 1) {
                        for (int i = 0; i < m; i++)
                            point[i] *= 0.5;
                    } else {
                        double norm = 0.0;
                        for (double v : point)
                            norm += v * v;
                        norm = Math.sqrt(norm);
                        for (int i = 0; i < m; i++)
                            point[i] /= norm;
                    }
                }
                return points;
            }
        };
    }
}
