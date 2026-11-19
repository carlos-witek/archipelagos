package org.carlos.archipelagos.core;

import java.util.Arrays;
import java.util.StringJoiner;

/** Integer genotype: used both for assignments (ALBP) and permutations (CVRP). */
public final class IntChromosome extends Chromosome {

    public final int[] genes;

    public IntChromosome(int length) {
        this.genes = new int[length];
    }

    public IntChromosome(int[] genes) {
        this.genes = genes.clone();
    }

    @Override
    public int length() {
        return genes.length;
    }

    @Override
    public IntChromosome copy() {
        return copyStateInto(new IntChromosome(genes));
    }

    @Override
    public String genesAsString() {
        StringJoiner joiner = new StringJoiner(";");
        for (int gene : genes)
            joiner.add(Integer.toString(gene));
        return joiner.toString();
    }

    @Override
    public boolean sameGenes(Chromosome other) {
        return other instanceof IntChromosome i && Arrays.equals(genes, i.genes);
    }
}
