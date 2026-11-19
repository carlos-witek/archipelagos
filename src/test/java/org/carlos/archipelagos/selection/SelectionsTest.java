package org.carlos.archipelagos.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.carlos.archipelagos.TestSupport.evaluated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.core.DoubleChromosome;
import org.carlos.archipelagos.param.Params;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SelectionsTest {

    private final SelectionCatalog catalog = new SelectionCatalog();

    static List<String> schemes() {
        return new SelectionCatalog().entries().stream().map(SelectionCatalog.Entry::name).toList();
    }

    private Selection create(String name) {
        SelectionCatalog.Entry entry = catalog.get(name);
        return entry.factory().apply(Params.resolve(entry.params(), Map.of()));
    }

    private static List<DoubleChromosome> randomPopulation(int n, SplittableRandom rng) {
        List<DoubleChromosome> pop = new ArrayList<>();
        for (int i = 0; i < n; i++)
            pop.add(evaluated(rng.nextDouble(), rng.nextDouble()));
        return pop;
    }

    @ParameterizedTest
    @MethodSource("schemes")
    void producesRequestedSizes(String name) {
        Selection selection = create(name);
        SelectionContext ctx = new SelectionContext(new SplittableRandom(1));
        List<DoubleChromosome> pop = randomPopulation(20, new SplittableRandom(2));
        List<DoubleChromosome> parents = selection.selectParents(pop, 10, ctx);
        assertThat(parents).hasSize(10);
        List<DoubleChromosome> offspring = randomPopulation(10, new SplittableRandom(3));
        assertThat(selection.survive(pop, offspring, 20, ctx)).hasSize(20);
    }

    @ParameterizedTest
    @MethodSource("schemes")
    void constantObjectivesDoNotProduceNaN(String name) {
        // 2008: division by (max - min) = 0 gave NaN fitness and a broken roulette
        Selection selection = create(name);
        List<DoubleChromosome> pop = new ArrayList<>();
        for (int i = 0; i < 6; i++)
            pop.add(evaluated(1.0, 1.0));
        List<DoubleChromosome> parents = selection.selectParents(pop, 4, new SelectionContext(new SplittableRandom(4)));
        assertThat(parents).hasSize(4);
        for (Chromosome c : pop)
            assertThat(c.fitness()).isNotNaN();
    }

    @ParameterizedTest
    @MethodSource("schemes")
    void tinyPopulationsWork(String name) {
        // 2008 SPEA: nextInt(0) for populations under 10
        Selection selection = create(name);
        List<DoubleChromosome> pop = randomPopulation(4, new SplittableRandom(9));
        assertThat(selection.selectParents(pop, 2, new SelectionContext(new SplittableRandom(5)))).hasSize(2);
    }

    @Test
    void npgaPrefersTheNondominatedCandidate() {
        Npga npga = new Npga(0.1, 10);
        DoubleChromosome good = evaluated(0, 0);
        List<DoubleChromosome> pop = new ArrayList<>();
        pop.add(good);
        for (int i = 0; i < 9; i++)
            pop.add(evaluated(1 + i, 1 + i));
        List<DoubleChromosome> parents = npga.selectParents(pop, 2000, new SelectionContext(new SplittableRandom(6)));
        long goodCount = parents.stream().filter(p -> p == good).count();
        // a candidate that is never dominated must win far more often than its 1/10 share
        assertThat(goodCount).isGreaterThan(300);
    }

    @Test
    void speaExternalSetIsNondominatedAndBounded() {
        Spea spea = new Spea(3);
        List<DoubleChromosome> pop = new ArrayList<>();
        for (int i = 0; i <= 10; i++)
            pop.add(evaluated(i, 10 - i)); // all mutually non-dominated
        pop.add(evaluated(20, 20));
        spea.selectParents(pop, 6, new SelectionContext(new SplittableRandom(7)));
        assertThat(spea.external()).hasSize(3);
        // clustering keeps the spread: both extremes' neighbourhoods are represented
        double min = spea.external().stream().mapToDouble(c -> c.objective(0)).min().orElseThrow();
        double max = spea.external().stream().mapToDouble(c -> c.objective(0)).max().orElseThrow();
        assertThat(min).isLessThanOrEqualTo(2);
        assertThat(max).isGreaterThanOrEqualTo(8);
    }

    @Test
    void nsga2SurvivalIsElitist() {
        Nsga2 nsga2 = new Nsga2();
        DoubleChromosome best = evaluated(0, 0);
        List<DoubleChromosome> pop = new ArrayList<>(List.of(best, evaluated(5, 5), evaluated(6, 6)));
        List<DoubleChromosome> offspring = List.of(evaluated(7, 7), evaluated(8, 8), evaluated(9, 9));
        List<DoubleChromosome> next = nsga2.survive(pop, offspring, 3, new SelectionContext(new SplittableRandom(8)));
        assertThat(next).containsExactlyInAnyOrder(best, pop.get(1), pop.get(2));
    }

    @Test
    void catalogIsChronologicalAndKeepsNsga2AsDefault() {
        List<SelectionCatalog.Entry> entries = catalog.entries();
        assertThat(entries).extracting(SelectionCatalog.Entry::year).isSorted();
        assertThat(entries).extracting(SelectionCatalog.Entry::name).containsExactly("vega", "random-objective",
                "wbga-ve", "wbga-sf", "moga", "npga", "nsga", "spea", "spea2", "nsga2", "nsga3", "spea2-sde");
        assertThat(catalog.get(SelectionCatalog.DEFAULT).year()).isEqualTo(2002);
    }

    @Test
    void spea2KeepsBoundaryPointsWhenTruncating() {
        for (boolean sde : new boolean[] { false, true }) {
            Spea2 spea2 = new Spea2(0, sde);
            List<DoubleChromosome> archive = new ArrayList<>();
            for (int i = 0; i <= 10; i++)
                archive.add(evaluated(i, 10 - i)); // 11 non-dominated points on a line
            List<DoubleChromosome> offspring = List.of(evaluated(20, 20), evaluated(4.5, 5.5));
            List<DoubleChromosome> next = spea2.survive(archive, offspring, 5, new SelectionContext(new SplittableRandom(1)));
            assertThat(next).hasSize(5);
            assertThat(next).noneMatch(c -> c.objective(0) == 20); // dominated, never kept over non-dominated
            assertThat(next).anyMatch(c -> c.objective(0) == 0).anyMatch(c -> c.objective(0) == 10);
        }
    }

    @Test
    void spea2FillsWithBestDominatedWhenFrontIsSmall() {
        Spea2 spea2 = new Spea2(0, false);
        DoubleChromosome best = evaluated(0, 0);
        DoubleChromosome second = evaluated(1, 1);
        List<DoubleChromosome> next = spea2.survive(List.of(best, evaluated(5, 5)),
                List.of(second, evaluated(9, 9)), 2, new SelectionContext(new SplittableRandom(2)));
        assertThat(next).containsExactlyInAnyOrder(best, second);
        assertThat(best.fitness()).isLessThan(1.0);
        assertThat(second.fitness()).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void nsga3ReferencePointsMatchPopulation() {
        Nsga3 nsga3 = new Nsga3(0);
        assertThat(nsga3.referencesFor(3, 100)).hasSize(91); // 12 divisions
        assertThat(new Nsga3(0).referencesFor(2, 100)).hasSize(100);
        assertThat(new Nsga3(4).referencesFor(3, 100)).hasSize(15);
    }

    @Test
    void nsga3SpreadsTheOverflowingFrontAcrossReferenceDirections() {
        Nsga3 nsga3 = new Nsga3(4); // directions at 0, .25, .5, .75, 1 for two objectives
        List<DoubleChromosome> crowded = new ArrayList<>();
        for (int i = 0; i < 6; i++)
            crowded.add(evaluated(0.01 * i, 1 - 0.01 * i)); // six points bunched near (0,1)
        List<DoubleChromosome> spread = List.of(evaluated(0.5, 0.5), evaluated(1, 0));
        List<DoubleChromosome> next = nsga3.survive(crowded, spread, 4, new SelectionContext(new SplittableRandom(3)));
        assertThat(next).hasSize(4);
        // the lone points in empty niches must win over the extra members of the crowded niche
        assertThat(next).contains(spread.get(0), spread.get(1));
    }

    @Test
    void rouletteFallsBackToUniformOnZeroTotal() {
        List<DoubleChromosome> pool = randomPopulation(5, new SplittableRandom(1));
        pool.forEach(c -> c.setFitness(0));
        assertThat(Selections.roulette(pool, 50, new SplittableRandom(2))).hasSize(50);
    }
}
