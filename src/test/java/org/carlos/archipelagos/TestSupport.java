package org.carlos.archipelagos;

import java.util.Map;

import org.carlos.archipelagos.core.DoubleChromosome;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;

public final class TestSupport {

    private TestSupport() {
    }

    public static DoubleChromosome evaluated(double... objectives) {
        DoubleChromosome c = new DoubleChromosome(1);
        c.setEvaluation(objectives, 0.0);
        return c;
    }

    public static DoubleChromosome infeasible(double violation, double... objectives) {
        DoubleChromosome c = new DoubleChromosome(1);
        c.setEvaluation(objectives, violation);
        return c;
    }

    @SuppressWarnings("unchecked")
    public static <C extends org.carlos.archipelagos.core.Chromosome> Problem<C> create(ProblemFactory factory,
            Map<String, Object> params) {
        return (Problem<C>) factory.create(Params.resolve(factory.params(), params));
    }
}
