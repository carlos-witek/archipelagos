package org.carlos.archipelagos.engine;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.carlos.archipelagos.migration.EmigrantPolicy;
import org.carlos.archipelagos.migration.ImmigrantPolicy;
import org.carlos.archipelagos.migration.Topology;
import org.carlos.archipelagos.selection.SelectionCatalog;

/**
 * Configuration of one evolution run (the 2008 {@code EConfiguration} + {@code ProblemContext}).
 * Missing optional values get defaults via {@link #withDefaults()}.
 *
 * @param offspring       offspring per generation and island; null = population for elitist
 *                        schemes, half the population for the classic ones
 * @param restartInterval re-initialise every island's population every N generations (0 = never);
 *                        2008 restarted on <i>every</i> generation once the threshold was reached
 * @param referencePoint  hypervolume reference point; null = derived from the reference front
 * @param seed            master seed; null = random (the chosen seed is reported back)
 */
public record RunRequest(
        @NotNull @Valid Component problem,
        @Valid Component selection,
        @Min(1) @Max(256) Integer islands,
        @Min(4) @Max(100000) Integer populationSize,
        @Min(2) @Max(1000000) Integer offspring,
        @Min(1) @Max(10000000) Integer generations,
        @Min(0) Integer restartInterval,
        @Valid Migration migration,
        double[] referencePoint,
        Long seed) {

    public record Component(@NotBlank String name, Map<String, Object> params) {
    }

    public record Migration(
            @Min(1) Integer interval,
            @DecimalMin("0.0") @DecimalMax("1.0") Double rate,
            EmigrantPolicy emigrants,
            ImmigrantPolicy immigrants,
            Topology topology,
            Boolean copy) {

        Migration withDefaults() {
            return new Migration(interval == null ? 5 : interval, rate == null ? 0.1 : rate,
                    emigrants == null ? EmigrantPolicy.BEST : emigrants,
                    immigrants == null ? ImmigrantPolicy.REPLACE_WORST : immigrants,
                    topology == null ? Topology.RING : topology, copy == null ? Boolean.TRUE : copy);
        }
    }

    public RunRequest withDefaults() {
        return new RunRequest(problem, selection == null ? new Component(SelectionCatalog.DEFAULT, Map.of()) : selection,
                islands == null ? 4 : islands, populationSize == null ? 100 : populationSize, offspring,
                generations == null ? 250 : generations, restartInterval == null ? 0 : restartInterval,
                (migration == null ? new Migration(null, null, null, null, null, null) : migration).withDefaults(),
                referencePoint, seed);
    }
}
