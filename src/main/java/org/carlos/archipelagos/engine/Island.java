package org.carlos.archipelagos.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ParetoFront;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.Variation;
import org.carlos.archipelagos.selection.Selection;
import org.carlos.archipelagos.selection.SelectionContext;

/**
 * One sub-population (the 2008 "stock"). Confined to one thread at a time: the coordinator
 * steps all islands in parallel, then migrates between them while none is running, so islands
 * share no mutable state and need no locks.
 */
final class Island<C extends Chromosome> {

    private final int index;
    private final Problem<C> problem;
    private final Selection selection;
    private final RandomGenerator rng;
    private final SelectionContext context;
    private final int populationSize;
    private final int offspringSize;
    private final int restartInterval;
    private final ParetoFront<C> archive = new ParetoFront<>();
    private List<C> population = List.of();
    private long evaluations;

    Island(int index, Problem<C> problem, Selection selection, RandomGenerator rng, int populationSize,
            int offspringSize, int restartInterval) {
        this.index = index;
        this.problem = problem;
        this.selection = selection;
        this.rng = rng;
        this.context = new SelectionContext(rng);
        this.populationSize = populationSize;
        this.offspringSize = offspringSize;
        this.restartInterval = restartInterval;
    }

    void initialize() {
        population = randomPopulation();
    }

    private List<C> randomPopulation() {
        List<C> fresh = new ArrayList<>(populationSize);
        for (int i = 0; i < populationSize; i++) {
            C c = problem.create(rng);
            problem.evaluate(c);
            fresh.add(c);
        }
        evaluations += populationSize;
        archive.addAll(fresh);
        return fresh;
    }

    @SuppressWarnings("unchecked")
    void step(int generation) {
        context.setGeneration(generation);
        if (restartInterval > 0 && generation > 0 && generation % restartInterval == 0)
            population = randomPopulation(); // the archive keeps everything found so far

        List<C> parents = selection.selectParents(population, offspringSize, context);
        List<C> offspring = new ArrayList<>(parents.size());
        for (C parent : parents)
            offspring.add((C) parent.copy());
        // shuffle so schemes that fill the pool in blocks (VEGA, WBGA-VE) still mix across blocks
        for (int i = offspring.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            C tmp = offspring.get(i);
            offspring.set(i, offspring.get(j));
            offspring.set(j, tmp);
        }
        Variation<C> variation = problem.variation();
        for (int i = 0; i + 1 < offspring.size(); i += 2)
            if (rng.nextDouble() < variation.crossoverRate())
                variation.crossover(offspring.get(i), offspring.get(i + 1), rng);
        for (C child : offspring) {
            variation.mutate(child, rng);
            child.resetEvaluation();
            problem.evaluate(child);
        }
        evaluations += offspring.size();
        population = selection.survive(population, offspring, populationSize, context);
        archive.addAll(offspring);
    }

    int index() {
        return index;
    }

    RandomGenerator rng() {
        return rng;
    }

    List<C> population() {
        return population;
    }

    void replacePopulation(List<C> population) {
        this.population = population;
    }

    ParetoFront<C> archive() {
        return archive;
    }

    long evaluations() {
        return evaluations;
    }
}
