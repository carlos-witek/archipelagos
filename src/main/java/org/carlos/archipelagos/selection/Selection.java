package org.carlos.archipelagos.selection;

import java.util.List;

import org.carlos.archipelagos.core.Chromosome;

/**
 * A multi-objective selection scheme (the 2008 {@code ISelection}).
 * One instance per island, so schemes may keep state (SPEA's external set, WBGA groups).
 */
public interface Selection {

    /**
     * Mating selection ("preselect" in 2008). Returns {@code count} parents; the engine copies
     * them before variation, so implementations may return the same individual several times.
     */
    <C extends Chromosome> List<C> selectParents(List<C> population, int count, SelectionContext ctx);

    /**
     * Environmental selection ("postselect"): builds the next population of {@code size}
     * from the current population and the evaluated offspring.
     */
    <C extends Chromosome> List<C> survive(List<C> population, List<C> offspring, int size, SelectionContext ctx);

    /**
     * True for (mu+lambda) elitist schemes, which by default produce a full population of
     * offspring every generation; the classic schemes default to half.
     */
    default boolean elitist() {
        return false;
    }
}
