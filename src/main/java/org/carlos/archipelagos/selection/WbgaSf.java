package org.carlos.archipelagos.selection;

import java.util.List;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * Weight-based GA with "sharing by group": every individual carries a weight vector (its group,
 * inherited by offspring); fitness is its weighted normalised score divided by the group size;
 * proportional selection over the whole population. Unassigned individuals (initial population,
 * restarts) join the smallest group.
 */
public final class WbgaSf extends ClassicSelection {

    private final String vectorsData;
    private List<double[]> vectors;

    public WbgaSf(String vectorsData) {
        this.vectorsData = vectorsData;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        if (vectors == null)
            vectors = WeightVectors.parseOrGenerate(vectorsData, space.dimensions());
        int[] groupSizes = new int[vectors.size()];
        for (C c : population)
            if (c.group() >= 0 && c.group() < vectors.size())
                groupSizes[c.group()]++;
            else
                c.setGroup(-1);
        for (C c : population)
            if (c.group() < 0) {
                int smallest = 0;
                for (int g = 1; g < groupSizes.length; g++)
                    if (groupSizes[g] < groupSizes[smallest])
                        smallest = g;
                c.setGroup(smallest);
                groupSizes[smallest]++;
            }
        for (C c : population) {
            double f = WbgaVe.weighted(c, vectors.get(c.group()), space) / groupSizes[c.group()];
            c.setFitness(Selections.constrain(f, c));
        }
        return Selections.roulette(population, count, ctx.rng());
    }
}
