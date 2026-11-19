package org.carlos.archipelagos.engine;

/**
 * Statistics of the collective front after one generation. Indicators that need a reference
 * front or point are {@code null} when unavailable.
 */
public record GenerationStats(
        int generation,
        long evaluations,
        int frontSize,
        Double hypervolume,
        Double generationalDistance,
        Double invertedGenerationalDistance,
        Double invertedGenerationalDistancePlus,
        Double maximumFrontError,
        Double spacing,
        Double maximumSpread,
        long elapsedMillis) {
}
