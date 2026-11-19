package org.carlos.archipelagos.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Where an emigrant from island {@code source} goes (the 2008 {@code IMigrationTopology}). */
public enum Topology {
    /** Unidirectional ring: i -> i+1 (2008 "circle"). */
    RING,
    /** A random other island, drawn per emigrant (2008 crashed with a single destination). */
    RANDOM,
    /** Every other island receives a copy. */
    FULL,
    /** Isolated islands. */
    NONE;

    public List<Integer> targets(int source, int islands, RandomGenerator rng) {
        if (islands < 2 || this == NONE)
            return List.of();
        return switch (this) {
        case RING -> List.of((source + 1) % islands);
        case RANDOM -> {
            int target = rng.nextInt(islands - 1);
            yield List.of(target >= source ? target + 1 : target);
        }
        case FULL -> {
            List<Integer> all = new ArrayList<>(islands - 1);
            for (int i = 0; i < islands; i++)
                if (i != source)
                    all.add(i);
            yield all;
        }
        case NONE -> List.of();
        };
    }
}
