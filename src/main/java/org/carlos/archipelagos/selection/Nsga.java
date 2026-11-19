package org.carlos.archipelagos.selection;

import java.util.List;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.NondominatedSorting;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * NSGA (Srinivas & Deb 1994): layered non-dominated sorting; each front gets a dummy
 * fitness, shared within the front; the next front starts just below the smallest shared
 * fitness of the previous one; proportional selection.
 * <p>
 * 2008 ranked by "number of dominators" (that is MOGA's rank, not NSGA's fronts) and walked
 * the ranks in {@code HashMap} order.
 */
public final class Nsga extends ClassicSelection {

    private final double sigmaShare;

    public Nsga(double sigmaShare) {
        this.sigmaShare = sigmaShare;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        double dummy = population.size();
        for (List<C> front : NondominatedSorting.sort(population)) {
            double minShared = dummy;
            for (C c : front) {
                double nicheCount = 0.0;
                for (C other : front)
                    nicheCount += ObjectiveSpace.share(space.distance(c, other), sigmaShare);
                double shared = dummy / nicheCount;
                c.setFitness(shared);
                minShared = Math.min(minShared, shared);
            }
            dummy = minShared * 0.95;
        }
        for (C c : population)
            c.setFitness(Selections.constrain(c.fitness(), c));
        return Selections.roulette(population, count, ctx.rng());
    }
}
