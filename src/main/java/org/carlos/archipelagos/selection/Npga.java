package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * NPGA (Horn, Nafpliotis & Goldberg 1994): Pareto domination tournaments. Two candidates are
 * checked against a random comparison set; a candidate that is <b>dominated by</b> some member
 * loses to one that is not (2008 tested the opposite relation). Ties are broken by the smaller
 * niche count against the mating pool built so far (continuously updated sharing).
 */
public final class Npga extends ClassicSelection {

    private final double sigmaShare;
    private final int comparisonSetSize;

    public Npga(double sigmaShare, int comparisonSetSize) {
        this.sigmaShare = sigmaShare;
        this.comparisonSetSize = comparisonSetSize;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        RandomGenerator rng = ctx.rng();
        ObjectiveSpace space = ObjectiveSpace.of(population);
        int n = population.size();
        List<C> pool = new ArrayList<>(count);
        while (pool.size() < count) {
            C a = population.get(rng.nextInt(n));
            C b = population.get(rng.nextInt(n));
            boolean aDominated = false;
            boolean bDominated = false;
            for (int t = 0; t < comparisonSetSize; t++) {
                C judge = population.get(rng.nextInt(n));
                aDominated |= Dominance.dominates(judge, a);
                bDominated |= Dominance.dominates(judge, b);
            }
            if (aDominated != bDominated) {
                pool.add(aDominated ? b : a);
                continue;
            }
            double nicheA = 0.0;
            double nicheB = 0.0;
            for (C member : pool) {
                nicheA += ObjectiveSpace.share(space.distance(a, member), sigmaShare);
                nicheB += ObjectiveSpace.share(space.distance(b, member), sigmaShare);
            }
            if (nicheA < nicheB)
                pool.add(a);
            else if (nicheB < nicheA)
                pool.add(b);
            else
                pool.add(rng.nextBoolean() ? a : b);
        }
        return pool;
    }
}
