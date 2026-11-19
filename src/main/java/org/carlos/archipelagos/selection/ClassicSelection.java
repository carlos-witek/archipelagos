package org.carlos.archipelagos.selection;

import java.util.List;

import org.carlos.archipelagos.core.Chromosome;

/** Base of the 2008 schemes: their own mating selection + random replacement survival. */
public abstract class ClassicSelection implements Selection {

    @Override
    public <C extends Chromosome> List<C> survive(List<C> population, List<C> offspring, int size,
            SelectionContext ctx) {
        return Selections.randomReplacement(population, offspring, size, ctx.rng());
    }
}
