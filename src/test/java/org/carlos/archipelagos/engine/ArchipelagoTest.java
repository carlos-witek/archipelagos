package org.carlos.archipelagos.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.carlos.archipelagos.migration.EmigrantPolicy;
import org.carlos.archipelagos.migration.ImmigrantPolicy;
import org.carlos.archipelagos.migration.Topology;
import org.carlos.archipelagos.problem.ProblemCatalog;
import org.carlos.archipelagos.problem.albp.AlbpFactory;
import org.carlos.archipelagos.problem.cvrp.CvrpFactory;
import org.carlos.archipelagos.problem.knapsack.KnapsackFactory;
import org.carlos.archipelagos.problem.real.DtlzFactory;
import org.carlos.archipelagos.problem.real.KursaweFactory;
import org.carlos.archipelagos.problem.real.SchafferFactory;
import org.carlos.archipelagos.problem.real.TwoBarTrussFactory;
import org.carlos.archipelagos.problem.real.ZdtFactory;
import org.carlos.archipelagos.selection.SelectionCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ArchipelagoTest {

    private final RunService service = new RunService(
            new ProblemCatalog(List.of(new SchafferFactory(), new KursaweFactory(), new ZdtFactory(), new DtlzFactory(),
                    new TwoBarTrussFactory(), new AlbpFactory(), new KnapsackFactory(), new CvrpFactory())),
            new SelectionCatalog(), null, new ArchipelagosProperties(null, null));

    static List<String> schemes() {
        return new SelectionCatalog().entries().stream().map(SelectionCatalog.Entry::name).toList();
    }

    private static RunRequest request(String problem, Map<String, Object> params, String selection, int islands,
            int generations, Long seed, RunRequest.Migration migration) {
        return new RunRequest(new RunRequest.Component(problem, params), new RunRequest.Component(selection, Map.of()),
                islands, 40, null, generations, 0, migration, null, seed);
    }

    private Run runNow(RunRequest request) {
        Run run = service.create(request);
        run.execute();
        assertThat(run.error()).isNull();
        assertThat(run.status()).isEqualTo(RunStatus.FINISHED);
        return run;
    }

    @Test
    void nsga2ConvergesOnZdt1() {
        Run run = runNow(new RunRequest(new RunRequest.Component("zdt", Map.of("variant", "1")), null, 2, 50, null, 150,
                0, null, null, 42L));
        GenerationStats last = run.latest();
        assertThat(last.generation()).isEqualTo(150);
        assertThat(last.invertedGenerationalDistance()).isLessThan(0.02);
        assertThat(last.hypervolume()).isGreaterThan(0.85); // optimum ~ 0.87 with reference point (1.1, 1.1)
        assertThat(run.history()).hasSize(151);
    }

    @Test
    void sameSeedSameResult() {
        RunRequest request = request("kursawe", Map.of(), "spea", 3, 30, 7L, null);
        Run a = runNow(request);
        Run b = runNow(request);
        assertThat(a.front()).hasSameSizeAs(b.front());
        for (int i = 0; i < a.front().size(); i++)
            assertThat(a.front().get(i).genes()).isEqualTo(b.front().get(i).genes());
        assertThat(a.latest().hypervolume()).isEqualTo(b.latest().hypervolume());
    }

    @ParameterizedTest
    @MethodSource("schemes")
    void everySchemeRunsAndImprovesSchaffer(String scheme) {
        Run run = runNow(request("schaffer", Map.of(), scheme, 2, 40, 1L, null));
        List<GenerationStats> history = run.history();
        assertThat(history.get(history.size() - 1).invertedGenerationalDistance())
                .isLessThan(history.get(0).invertedGenerationalDistance());
    }

    @ParameterizedTest
    @ValueSource(strings = { "two-bar-truss", "albp", "knapsack", "cvrp", "dtlz", "kursawe" })
    void everyProblemRuns(String problem) {
        Map<String, Object> params = problem.equals("knapsack") ? Map.of("items", 100) : Map.of();
        Run run = runNow(request(problem, params, "nsga2", 2, 10, 3L, null));
        assertThat(run.front()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(Topology.class)
    void everyTopologyWorks(Topology topology) {
        for (EmigrantPolicy emigrants : EmigrantPolicy.values())
            for (ImmigrantPolicy immigrants : ImmigrantPolicy.values())
                for (boolean copy : new boolean[] { true, false }) {
                    Run run = runNow(request("zdt", Map.of("variant", "2"), "nsga2", 3, 12, 11L,
                            new RunRequest.Migration(2, 0.2, emigrants, immigrants, topology, copy)));
                    assertThat(run.front()).isNotEmpty();
                }
    }

    @Test
    void singleIslandAndTinyMigrationRateDoNotCrash() {
        // 2008: nextInt(0) in random topology with one destination / rate * size < 1
        runNow(request("schaffer", Map.of(), "vega", 1, 5, 2L,
                new RunRequest.Migration(1, 0.01, EmigrantPolicy.RANDOM, ImmigrantPolicy.ADD, Topology.RANDOM, false)));
        runNow(request("schaffer", Map.of(), "vega", 2, 5, 2L,
                new RunRequest.Migration(1, 0.01, EmigrantPolicy.RANDOM, ImmigrantPolicy.ADD, Topology.RANDOM, false)));
    }

    @Test
    void restartIntervalKeepsTheArchive() {
        Run run = runNow(new RunRequest(new RunRequest.Component("zdt", Map.of()), null, 1, 30, null, 40, 10, null,
                null, 5L));
        List<GenerationStats> history = run.history();
        // the collective front never gets worse, even right after a restart
        for (int i = 1; i < history.size(); i++)
            assertThat(history.get(i).hypervolume()).isGreaterThanOrEqualTo(history.get(i - 1).hypervolume() - 1e-12);
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThatThrownBy(() -> service.create(request("nope", Map.of(), "nsga2", 1, 1, 1L, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown problem");
        assertThatThrownBy(() -> service.create(request("zdt", Map.of("bogus", 1), "nsga2", 1, 1, 1L, null)))
                .hasMessageContaining("Unknown parameter");
        assertThatThrownBy(() -> service.create(request("zdt", Map.of("crossoverRate", 2), "nsga2", 1, 1, 1L, null)))
                .hasMessageContaining("crossoverRate");
    }
}
