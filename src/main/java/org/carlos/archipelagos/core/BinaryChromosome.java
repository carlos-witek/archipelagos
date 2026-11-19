package org.carlos.archipelagos.core;

import java.util.Arrays;

/** Bit-string genotype. */
public final class BinaryChromosome extends Chromosome {

    public final boolean[] genes;

    public BinaryChromosome(int length) {
        this.genes = new boolean[length];
    }

    public BinaryChromosome(boolean[] genes) {
        this.genes = genes.clone();
    }

    @Override
    public int length() {
        return genes.length;
    }

    @Override
    public BinaryChromosome copy() {
        return copyStateInto(new BinaryChromosome(genes));
    }

    @Override
    public String genesAsString() {
        StringBuilder builder = new StringBuilder(genes.length);
        for (boolean gene : genes)
            builder.append(gene ? '1' : '0');
        return builder.toString();
    }

    @Override
    public boolean sameGenes(Chromosome other) {
        return other instanceof BinaryChromosome b && Arrays.equals(genes, b.genes);
    }
}
