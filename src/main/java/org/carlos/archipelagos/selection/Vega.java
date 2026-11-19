package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.List;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * VEGA (Schaffer 1985): the mating pool is filled in k equal parts, part i selected
 * proportionally to objective i <b>from the whole population</b>.
 * (2008 dealt the population into k disjoint groups first, so each individual was only
 * ever judged on one objective.)
 */
public final class Vega extends ClassicSelection {

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        int m = space.dimensions();
        int[] parts = Selections.split(count, m);
        List<C> pool = new ArrayList<>(count);
        for (int k = 0; k < m; k++) {
            for (C c : population)
                c.setFitness(Selections.constrain(objectiveFitness(c, k, space), c));
            pool.addAll(Selections.roulette(population, parts[k], ctx.rng()));
        }
        return pool;
    }

    /**
     * Distance from the worst value, normalised, plus a small floor so the worst individual
     * keeps a non-zero chance (2008: {@code max - value} gave it exactly zero).
     */
    static double objectiveFitness(Chromosome c, int k, ObjectiveSpace space) {
        return (1.0 - space.normalized(c, k)) + 0.01;
    }
}
