package org.carlos.archipelagos.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.Dominance;
import org.carlos.archipelagos.core.ObjectiveSpace;

/**
 * MOGA (Fonseca & Fleming 1993): rank = 1 + number of dominators; raw fitness interpolated
 * over the sorted population and averaged per rank; fitness sharing within each rank, rescaled
 * so the rank keeps its total; proportional selection.
 */
public final class Moga extends ClassicSelection {

    private final double sigmaShare;

    public Moga(double sigmaShare) {
        this.sigmaShare = sigmaShare;
    }

    @Override
    public <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx) {
        ObjectiveSpace space = ObjectiveSpace.of(population);
        int n = population.size();
        Map<Integer, List<C>> byRank = new TreeMap<>();
        for (C c : population) {
            int dominators = 0;
            for (C other : population)
                if (Dominance.dominates(other, c))
                    dominators++;
            byRank.computeIfAbsent(dominators, r -> new ArrayList<>()).add(c);
        }
        int position = 0; // individuals already placed in better ranks
        for (List<C> rank : byRank.values()) {
            int size = rank.size();
            // average of linear raw fitness n - position ... n - position - size + 1
            double raw = n - position - (size - 1) / 2.0;
            double sharedSum = 0.0;
            for (C c : rank) {
                double nicheCount = 0.0;
                for (C other : rank)
                    nicheCount += ObjectiveSpace.share(space.distance(c, other), sigmaShare);
                c.setFitness(raw / nicheCount); // nicheCount >= 1 (self)
                sharedSum += c.fitness();
            }
            double scale = raw * size / sharedSum;
            for (C c : rank)
                c.setFitness(Selections.constrain(c.fitness() * scale, c));
            position += size;
        }
        List<C> sorted = new ArrayList<>(population);
        sorted.sort(Comparator.comparingDouble(Chromosome::fitness));
        return Selections.roulette(sorted, count, ctx.rng());
    }
}
