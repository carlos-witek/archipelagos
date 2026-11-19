package org.carlos.archipelagos.selection;

import java.util.random.RandomGenerator;

/** Per-island context handed to a selection scheme. */
public final class SelectionContext {

    private final RandomGenerator rng;
    private int generation;

    public SelectionContext(RandomGenerator rng) {
        this.rng = rng;
    }

    public RandomGenerator rng() {
        return rng;
    }

    public int generation() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
