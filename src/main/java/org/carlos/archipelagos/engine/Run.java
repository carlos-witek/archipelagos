package org.carlos.archipelagos.engine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** One evolution run: its configuration, live state and listeners. Thread-safe for readers. */
public final class Run {

    private static final Logger log = LoggerFactory.getLogger(Run.class);

    /** Pushed to listeners after every generation and once more when the run ends. */
    public record Event(UUID runId, RunStatus status, GenerationStats stats, List<FrontPoint> front) {
    }

    private final UUID id = UUID.randomUUID();
    private final RunRequest config;
    private final long seed;
    private final Archipelago<?> archipelago;
    private final Instant createdAt = Instant.now();
    private final List<GenerationStats> history = new CopyOnWriteArrayList<>();
    private final List<Consumer<Event>> listeners = new CopyOnWriteArrayList<>();
    private volatile RunStatus status = RunStatus.QUEUED;
    private volatile String error;
    private volatile boolean stopRequested;
    private volatile List<FrontPoint> front = List.of();
    private volatile Instant startedAt;
    private volatile Instant finishedAt;

    Run(RunRequest config, long seed, Archipelago<?> archipelago) {
        this.config = config;
        this.seed = seed;
        this.archipelago = archipelago;
    }

    /** Runs to completion on the calling thread. */
    void execute() {
        if (stopRequested) {
            finish(RunStatus.STOPPED, null);
            return;
        }
        status = RunStatus.RUNNING;
        startedAt = Instant.now();
        long start = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            archipelago.initialize(executor);
            publish(0, start);
            for (int generation = 1; generation <= config.generations(); generation++) {
                if (stopRequested || Thread.currentThread().isInterrupted()) {
                    finish(RunStatus.STOPPED, null);
                    return;
                }
                archipelago.step(executor, generation);
                publish(generation, start);
            }
            finish(RunStatus.FINISHED, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finish(RunStatus.STOPPED, null);
        } catch (ExecutionException | RuntimeException e) {
            Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            log.error("Run {} failed", id, cause);
            finish(RunStatus.FAILED, cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    private void publish(int generation, long startNanos) {
        GenerationStats stats = archipelago.stats(generation, (System.nanoTime() - startNanos) / 1_000_000);
        history.add(stats);
        front = archipelago.frontPoints();
        fire(new Event(id, status, stats, front));
    }

    private void finish(RunStatus terminal, String message) {
        error = message;
        finishedAt = Instant.now();
        status = terminal;
        fire(new Event(id, terminal, latest(), front));
    }

    private void fire(Event event) {
        for (Consumer<Event> listener : listeners)
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                listeners.remove(listener);
            }
    }

    public void addListener(Consumer<Event> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Event> listener) {
        listeners.remove(listener);
    }

    public void requestStop() {
        stopRequested = true;
    }

    public UUID id() {
        return id;
    }

    public RunRequest config() {
        return config;
    }

    public long seed() {
        return seed;
    }

    public String problemName() {
        return archipelago.problem().name();
    }

    public List<String> objectives() {
        return archipelago.problem().objectives();
    }

    public RunStatus status() {
        return status;
    }

    public String error() {
        return error;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public List<GenerationStats> history() {
        return List.copyOf(history);
    }

    public GenerationStats latest() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    public List<FrontPoint> front() {
        return front;
    }

    public List<double[]> referenceFront() {
        return archipelago.reference();
    }

    public double[] referencePoint() {
        return archipelago.referencePoint();
    }
}
