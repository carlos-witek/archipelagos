package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * Weight-based GA, "vector evaluated" variant: for each weight vector the population is scored
 * by the weighted sum of <b>normalised</b> objectives (2008 used raw values, so the objective with
 * the largest scale dominated every vector); the best share of the population for that vector
 * enters a proportional selection for its share of the mating pool.
 */
public final class WbgaVe extends ClassicSelection {

    private final String vectorsData;
    private List<double[]> vectors;

    public WbgaVe(String vectorsData) {
        this.vectorsData = vectorsData;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        if (vectors == null)
            vectors = WeightVectors.parseOrGenerate(vectorsData, space.dimensions());
        int[] topSizes = Selections.split(population.size(), vectors.size());
        int[] poolSizes = Selections.split(count, vectors.size());
        List<C> pool = new ArrayList<>(count);
        List<C> sorted = new ArrayList<>(population);
        for (int v = 0; v < vectors.size(); v++) {
            double[] w = vectors.get(v);
            for (C c : population)
                c.setFitness(Selections.constrain(weighted(c, w, space), c));
            sorted.sort(Comparator.comparingDouble(Chromosome::fitness).reversed());
            List<C> top = sorted.subList(0, Math.max(1, topSizes[v]));
            pool.addAll(Selections.roulette(new ArrayList<>(top), poolSizes[v], ctx.rng()));
        }
        return pool;
    }

    static double weighted(Chromosome c, double[] w, ObjectiveSpace space) {
        double f = 0.0;
        for (int k = 0; k < w.length; k++)
            f += (1.0 - space.normalized(c, k)) * w[k];
        return f + 0.01;
    }
}
