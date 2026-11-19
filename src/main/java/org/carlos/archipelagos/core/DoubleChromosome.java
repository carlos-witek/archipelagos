package org.carlos.archipelagos.core;

import java.util.Arrays;
import java.util.StringJoiner;

/** Real-valued genotype. */
public final class DoubleChromosome extends Chromosome {

    public final double[] genes;

    public DoubleChromosome(int length) {
        this.genes = new double[length];
    }

    public DoubleChromosome(double[] genes) {
        this.genes = genes.clone();
    }

    @Override
    public int length() {
        return genes.length;
    }

    @Override
    public DoubleChromosome copy() {
        return copyStateInto(new DoubleChromosome(genes));
    }

    @Override
    public String genesAsString() {
        StringJoiner joiner = new StringJoiner(";");
        for (double gene : genes)
            joiner.add(Double.toString(gene));
        return joiner.toString();
    }

    @Override
    public boolean sameGenes(Chromosome other) {
        return other instanceof DoubleChromosome d && Arrays.equals(genes, d.genes);
    }
}
