package org.carlos.archipelagos.api;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.carlos.archipelagos.engine.GenerationStats;
import org.carlos.archipelagos.engine.ResultExporter;
import org.carlos.archipelagos.engine.Run;
import org.carlos.archipelagos.engine.RunRequest;
import org.carlos.archipelagos.engine.RunService;
import org.carlos.archipelagos.engine.RunView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

/**
 * Run lifecycle. Replaces the 2008 RMI {@code ServerEnvironmentInterface} (start/stop evolution,
 * remote listeners) with REST + Server-Sent Events.
 */
@RestController
@RequestMapping("/api/runs")
@Validated
public class RunController {

    /** Live updates are throttled to this interval; the final event is always sent. */
    private static final long SSE_INTERVAL_MILLIS = 200;

    private final RunService runs;

    public RunController(RunService runs) {
        this.runs = runs;
    }

    @PostMapping
    public ResponseEntity<RunView> start(@Valid @RequestBody RunRequest request) {
        Run run = runs.start(request);
        return ResponseEntity.accepted().location(URI.create("/api/runs/" + run.id())).body(RunView.of(run, false));
    }

    @GetMapping
    public List<RunView> list() {
        return runs.list().stream().map(run -> RunView.of(run, false)).toList();
    }

    @GetMapping("/{id}")
    public RunView get(@PathVariable UUID id) {
        return RunView.of(run(id), true);
    }

    @GetMapping("/{id}/stats")
    public List<GenerationStats> stats(@PathVariable UUID id) {
        return run(id).history();
    }

    @GetMapping(value = "/{id}/front.csv", produces = "text/csv")
    public String frontCsv(@PathVariable UUID id) {
        Run run = run(id);
        return ResultExporter.frontCsv(run.objectives(), run.front());
    }

    @GetMapping(value = "/{id}/stats.csv", produces = "text/csv")
    public String statsCsv(@PathVariable UUID id) {
        return ResultExporter.statsCsv(run(id).history());
    }

    @GetMapping("/{id}/reference")
    public CatalogController.Reference reference(@PathVariable UUID id) {
        Run run = run(id);
        List<double[]> front = run.referenceFront();
        return new CatalogController.Reference(run.problemName(), run.objectives(), front == null ? List.of() : front);
    }

    @PostMapping("/{id}/stop")
    public RunView stop(@PathVariable UUID id) {
        Run run = run(id);
        run.requestStop();
        return RunView.of(run, false);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return runs.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Server-Sent Events: "generation" events while running, then one "end" event. */
    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID id) {
        Run run = run(id);
        SseEmitter emitter = new SseEmitter(0L);
        AtomicLong lastSent = new AtomicLong();
        Consumer<Run.Event> listener = new Consumer<>() {
            @Override
            public void accept(Run.Event event) {
                boolean terminal = event.status().isTerminal();
                long now = System.currentTimeMillis();
                if (!terminal && now - lastSent.get() < SSE_INTERVAL_MILLIS)
                    return;
                lastSent.set(now);
                try {
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event().name(terminal ? "end" : "generation").data(event));
                    }
                    if (terminal) {
                        run.removeListener(this);
                        emitter.complete();
                    }
                } catch (IOException | IllegalStateException e) {
                    run.removeListener(this);
                }
            }
        };
        emitter.onCompletion(() -> run.removeListener(listener));
        emitter.onTimeout(() -> run.removeListener(listener));
        emitter.onError(e -> run.removeListener(listener));
        run.addListener(listener);
        // current state immediately, so late subscribers see something (and finished runs complete)
        listener.accept(new Run.Event(run.id(), run.status(), run.latest(), run.front()));
        return emitter;
    }

    private Run run(UUID id) {
        return runs.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No run " + id));
    }
}
