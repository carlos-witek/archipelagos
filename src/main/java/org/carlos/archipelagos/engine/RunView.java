package org.carlos.archipelagos.engine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** API/export representation of a run. */
public record RunView(
        UUID id,
        RunStatus status,
        String problem,
        List<String> objectives,
        long seed,
        RunRequest config,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String error,
        GenerationStats latest,
        List<FrontPoint> front) {

    public static RunView of(Run run, boolean includeFront) {
        return new RunView(run.id(), run.status(), run.problemName(), run.objectives(), run.seed(), run.config(),
                run.createdAt(), run.startedAt(), run.finishedAt(), run.error(), run.latest(),
                includeFront ? run.front() : null);
    }
}
