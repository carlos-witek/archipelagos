package org.carlos.archipelagos.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Unbounded archive of feasible, mutually non-dominated solutions.
 * Members are private copies, so later changes to the population never leak in.
 * Two members with identical objective vectors are kept only once (objective-space
 * de-duplication, as in 2008, but explicit instead of via {@code equals}).
 * <p>
 * Not thread-safe: each archive is confined to one island or to the run's coordinator.
 */
public final class ParetoFront<C extends Chromosome> implements Iterable<C> {

    private final List<C> members = new ArrayList<>();

    /** @return true if the archive changed. */
    @SuppressWarnings("unchecked")
    public boolean add(C candidate) {
        if (!candidate.isEvaluated() || !candidate.isFeasible())
            return false;
        double[] objectives = candidate.objectives();
        for (Iterator<C> it = members.iterator(); it.hasNext();) {
            double[] member = it.next().objectives();
            int cmp = Dominance.pareto(objectives, member);
            if (cmp > 0 || (cmp == 0 && Arrays.equals(objectives, member)))
                return false;
        }
        members.removeIf(member -> Dominance.pareto(objectives, member.objectives()) < 0);
        members.add((C) candidate.copy());
        return true;
    }

    public boolean addAll(Iterable<? extends C> candidates) {
        boolean changed = false;
        for (C candidate : candidates)
            changed |= add(candidate);
        return changed;
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public void clear() {
        members.clear();
    }

    /** Read-only view of the members (do not mutate them). */
    public List<C> members() {
        return Collections.unmodifiableList(members);
    }

    /** Objective vectors only, copied. */
    public List<double[]> points() {
        List<double[]> points = new ArrayList<>(members.size());
        for (C member : members)
            points.add(member.objectives().clone());
        return points;
    }

    @Override
    public Iterator<C> iterator() {
        return members().iterator();
    }
}
