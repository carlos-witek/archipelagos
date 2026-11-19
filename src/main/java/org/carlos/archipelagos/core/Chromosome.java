package org.carlos.archipelagos.core;

import java.util.Arrays;

/**
 * An individual: a genotype plus its evaluation.
 * <p>
 * All objectives are <b>minimised</b>. Constraints are expressed as a non-negative
 * {@link #violation()} (0 = feasible) and handled by constrained domination
 * (see {@link Dominance}) instead of the 2008 multiplicative penalty.
 * <p>
 * Unlike the 2008 version, equality is identity: objective-space de-duplication is done
 * explicitly by {@link ParetoFront}, so chromosomes are never mutable hash keys.
 * Fields below the evaluation ({@code fitness}, {@code rank}, {@code crowding}) are scratch
 * values owned by the current selection scheme.
 */
public abstract sealed class Chromosome permits DoubleChromosome, IntChromosome, BinaryChromosome {

    private static final double[] UNEVALUATED = new double[0];

    private double[] objectives = UNEVALUATED;
    private double violation;

    // selection scratch space
    private double fitness;
    private int rank;
    private double crowding;
    private int group = -1;

    public abstract int length();

    /** Deep copy of genes and evaluation. */
    public abstract Chromosome copy();

    public abstract String genesAsString();

    public abstract boolean sameGenes(Chromosome other);

    protected final <T extends Chromosome> T copyStateInto(T target) {
        Chromosome c = target;
        c.objectives = objectives.clone();
        c.violation = violation;
        c.fitness = fitness;
        c.rank = rank;
        c.crowding = crowding;
        c.group = group;
        return target;
    }

    public boolean isEvaluated() {
        return objectives != UNEVALUATED;
    }

    /** Called by the engine after variation, so no stale evaluation survives into offspring. */
    public void resetEvaluation() {
        objectives = UNEVALUATED;
        violation = 0.0;
        fitness = 0.0;
        rank = 0;
        crowding = 0.0;
    }

    public void setEvaluation(double[] objectives, double violation) {
        if (violation < 0 || Double.isNaN(violation))
            throw new IllegalArgumentException("violation must be >= 0, was " + violation);
        this.objectives = objectives.clone();
        this.violation = violation;
    }

    public double[] objectives() {
        return objectives;
    }

    public double objective(int index) {
        return objectives[index];
    }

    public int objectiveCount() {
        return objectives.length;
    }

    public double violation() {
        return violation;
    }

    public boolean isFeasible() {
        return violation == 0.0;
    }

    public double fitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public int rank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double crowding() {
        return crowding;
    }

    public void setCrowding(double crowding) {
        this.crowding = crowding;
    }

    public int group() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + genesAsString() + "] -> " + Arrays.toString(objectives)
                + (violation > 0 ? " (violation " + violation + ")" : "");
    }
}
