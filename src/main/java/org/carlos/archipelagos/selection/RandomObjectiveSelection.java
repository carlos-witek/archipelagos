package org.carlos.archipelagos.selection;

import java.util.List;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ObjectiveSpace;

/** Each generation one randomly chosen objective drives proportional selection. */
public final class RandomObjectiveSelection extends ClassicSelection {

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        int k = ctx.rng().nextInt(space.dimensions());
        for (C c : population)
            c.setFitness(Selections.constrain(Vega.objectiveFitness(c, k, space), c));
        return Selections.roulette(population, count, ctx.rng());
    }
}
