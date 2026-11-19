package org.carlos.archipelagos.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import org.carlos.archipelagos.core.Chromosome;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemCatalog;
import org.carlos.archipelagos.selection.Selection;
import org.carlos.archipelagos.selection.SelectionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/** Creates, schedules and keeps runs (in memory; finished runs are also exported to disk). */
@Service
public class RunService {

    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final ProblemCatalog problems;
    private final SelectionCatalog selections;
    private final ResultExporter exporter;
    private final Semaphore slots;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, Run> runs = new ConcurrentHashMap<>();

    public RunService(ProblemCatalog problems, SelectionCatalog selections, ResultExporter exporter,
            ArchipelagosProperties properties) {
        this.problems = problems;
        this.selections = selections;
        this.exporter = exporter;
        this.slots = new Semaphore(properties.maxConcurrentRuns(), true);
    }

    /** Validates the request, builds the run and queues it. Throws IllegalArgumentException on bad config. */
    public Run start(RunRequest request) {
        Run run = create(request);
        runs.put(run.id(), run);
        executor.submit(() -> {
            try {
                slots.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                run.execute();
                if (run.status() != RunStatus.FAILED || run.latest() != null)
                    exporter.export(run);
            } catch (RuntimeException e) {
                log.error("Exporting run {} failed", run.id(), e);
            } finally {
                slots.release();
            }
        });
        return run;
    }

    /** Builds a run without scheduling it (used by tests and {@link #start}). */
    public Run create(RunRequest request) {
        RunRequest config = request.withDefaults();
        long seed = config.seed() != null ? config.seed() : ThreadLocalRandom.current().nextLong();
        Problem<?> problem = problems.create(config.problem().name(), config.problem().params());
        SelectionCatalog.Entry entry = selections.get(config.selection().name());
        Params selectionParams = Params.resolve(entry.params(), config.selection().params());
        return new Run(config, seed, archipelago(problem, config, entry, selectionParams, seed));
    }

    private static <C extends Chromosome> Archipelago<C> archipelago(Problem<C> problem, RunRequest config,
            SelectionCatalog.Entry entry, Params params, long seed) {
        return new Archipelago<>(problem, config, i -> {
            Selection selection = entry.factory().apply(params);
            return selection;
        }, seed);
    }

    public Optional<Run> find(UUID id) {
        return Optional.ofNullable(runs.get(id));
    }

    public List<Run> list() {
        List<Run> all = new ArrayList<>(runs.values());
        all.sort(Comparator.comparing(Run::createdAt).reversed());
        return all;
    }

    public boolean delete(UUID id) {
        Run run = runs.get(id);
        if (run == null)
            return false;
        run.requestStop();
        runs.remove(id);
        return true;
    }

    @PreDestroy
    void shutdown() {
        runs.values().forEach(Run::requestStop);
        executor.shutdownNow();
    }
}
