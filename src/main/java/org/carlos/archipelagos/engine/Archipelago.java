package org.carlos.archipelagos.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.ParetoFront;
import org.carlos.archipelagos.metrics.Metrics;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.selection.Selection;

/**
 * The island model: N islands evolve in parallel (one virtual thread each per generation),
 * migration happens synchronously between generations, the collective front merges all island
 * archives.
 * <p>
 * Replaces the 2008 environment/deme/stock hierarchy (two RMI or JBoss servers) with one
 * in-process level of islands. Because islands are stepped in lock-step and every random
 * stream is split from one master seed, a run is fully reproducible from its seed.
 */
final class Archipelago<C extends Chromosome> {

    private final Problem<C> problem;
    private final RunRequest config;
    private final List<Island<C>> islands = new ArrayList<>();
    private final RandomGenerator migrationRng;
    private final ParetoFront<C> front = new ParetoFront<>();
    private final List<double[]> reference;
    private final double[] referencePoint;

    Archipelago(Problem<C> problem, RunRequest config, IntFunction<Selection> selections, long seed) {
        this.problem = problem;
        this.config = config;
        SplittableRandom master = new SplittableRandom(seed);
        int population = config.populationSize();
        Selection probe = selections.apply(-1);
        int offspring = config.offspring() != null ? config.offspring()
                : probe.elitist() ? population : Math.max(2, population / 2);
        if (!probe.elitist() && offspring > population)
            throw new IllegalArgumentException("Classic selection schemes need offspring <= populationSize");
        for (int i = 0; i < config.islands(); i++)
            islands.add(new Island<>(i, problem, selections.apply(i), master.split(), population, offspring,
                    config.restartInterval()));
        this.migrationRng = master.split();
        this.reference = problem.referenceFront().filter(r -> !r.isEmpty()).orElse(null);
        double[] point = config.referencePoint();
        if (point != null && point.length != problem.objectives().size())
            throw new IllegalArgumentException("referencePoint needs " + problem.objectives().size() + " values");
        this.referencePoint = point != null ? point.clone()
                : reference != null ? Metrics.referencePointFor(reference) : null;
    }

    void initialize(ExecutorService executor) throws InterruptedException, ExecutionException {
        runAll(executor, island -> {
            island.initialize();
            return null;
        });
        mergeFront();
    }

    void step(ExecutorService executor, int generation) throws InterruptedException, ExecutionException {
        runAll(executor, island -> {
            island.step(generation);
            return null;
        });
        RunRequest.Migration migration = config.migration();
        if (islands.size() > 1 && generation % migration.interval() == 0)
            migrate(migration);
        mergeFront();
    }

    private interface IslandTask<C extends Chromosome> {
        Void apply(Island<C> island) throws Exception;
    }

    private void runAll(ExecutorService executor, IslandTask<C> task) throws InterruptedException, ExecutionException {
        List<Callable<Void>> calls = new ArrayList<>(islands.size());
        for (Island<C> island : islands)
            calls.add(() -> task.apply(island));
        for (Future<Void> future : executor.invokeAll(calls))
            future.get(); // rethrows the first island failure
    }

    @SuppressWarnings("unchecked")
    private void migrate(RunRequest.Migration migration) {
        int n = islands.size();
        List<List<C>> inboxes = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            inboxes.add(new ArrayList<>());
        for (Island<C> island : islands) {
            List<C> population = island.population();
            int count = Math.min(population.size() - 1, Math.max(1, (int) Math.round(migration.rate() * population.size())));
            if (count <= 0)
                continue;
            List<C> emigrants = migration.emigrants().choose(population, count, migrationRng);
            boolean sent = false;
            for (C emigrant : emigrants)
                for (int target : migration.topology().targets(island.index(), n, migrationRng)) {
                    inboxes.get(target).add((C) emigrant.copy());
                    sent = true;
                }
            if (sent && !migration.copy()) {
                List<C> remaining = new ArrayList<>(population);
                remaining.removeAll(emigrants);
                island.replacePopulation(remaining);
            }
        }
        for (Island<C> island : islands)
            island.replacePopulation(
                    migration.immigrants().integrate(island.population(), inboxes.get(island.index()), migrationRng));
    }

    private void mergeFront() {
        for (Island<C> island : islands)
            front.addAll(island.archive());
    }

    GenerationStats stats(int generation, long elapsedMillis) {
        long evaluations = 0;
        for (Island<C> island : islands)
            evaluations += island.evaluations();
        List<double[]> points = front.points();
        if (points.isEmpty())
            return new GenerationStats(generation, evaluations, 0, null, null, null, null, null, null, null, elapsedMillis);
        int m = problem.objectives().size();
        Double hv = referencePoint != null && m <= 4 ? Metrics.hypervolume(points, referencePoint) : null;
        Double gd = null, igd = null, igdPlus = null, mfe = null;
        if (reference != null) {
            gd = Metrics.generationalDistance(points, reference);
            igd = Metrics.invertedGenerationalDistance(points, reference);
            igdPlus = Metrics.invertedGenerationalDistancePlus(points, reference);
            mfe = Metrics.maximumFrontError(points, reference);
        }
        return new GenerationStats(generation, evaluations, points.size(), hv, gd, igd, igdPlus, mfe,
                Metrics.spacing(points), Metrics.maximumSpread(points), elapsedMillis);
    }

    List<FrontPoint> frontPoints() {
        List<FrontPoint> result = new ArrayList<>(front.size());
        for (C c : front)
            result.add(new FrontPoint(c.objectives().clone(), c.genesAsString()));
        return result;
    }

    List<double[]> reference() {
        return reference;
    }

    double[] referencePoint() {
        return referencePoint == null ? null : referencePoint.clone();
    }

    Problem<C> problem() {
        return problem;
    }
}
