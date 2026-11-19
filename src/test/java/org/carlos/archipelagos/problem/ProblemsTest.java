package org.carlos.archipelagos.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.carlos.archipelagos.TestSupport;
import org.carlos.archipelagos.core.BinaryChromosome;
import org.carlos.archipelagos.core.DoubleChromosome;
import org.carlos.archipelagos.core.IntChromosome;
import org.carlos.archipelagos.problem.albp.AlbpData;
import org.carlos.archipelagos.problem.albp.AlbpFactory;
import org.carlos.archipelagos.problem.cvrp.CvrpFactory;
import org.carlos.archipelagos.problem.knapsack.KnapsackFactory;
import org.carlos.archipelagos.problem.real.DtlzFactory;
import org.carlos.archipelagos.problem.real.KursaweFactory;
import org.carlos.archipelagos.problem.real.SchafferFactory;
import org.carlos.archipelagos.problem.real.TwoBarTrussFactory;
import org.carlos.archipelagos.problem.real.ZdtFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProblemsTest {

    static final List<String> ALBP_DATA_SETS = AlbpFactory.DATA_SETS;

    private static DoubleChromosome real(double... genes) {
        return new DoubleChromosome(genes);
    }

    @Test
    void schafferAndItsFront() {
        Problem<DoubleChromosome> p = TestSupport.create(new SchafferFactory(), Map.of());
        DoubleChromosome c = real(1, 1);
        p.evaluate(c);
        assertThat(c.objectives()).containsExactly(0.0, 18.0);
        assertThat(p.referenceFront()).isPresent();
        assertThat(p.referenceFront().get()).anySatisfy(pt -> assertThat(pt).containsExactly(0.0, 18.0));
    }

    @Test
    void kursaweAtOriginAndBundledReference() {
        Problem<DoubleChromosome> p = TestSupport.create(new KursaweFactory(), Map.of());
        DoubleChromosome c = real(0, 0, 0);
        p.evaluate(c);
        assertThat(c.objective(0)).isCloseTo(-20.0, within(1e-12));
        assertThat(c.objective(1)).isCloseTo(0.0, within(1e-12));
        assertThat(p.referenceFront().orElseThrow()).hasSizeGreaterThan(100);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "2", "3", "4", "6" })
    void zdtOptimalPointsLieOnTheReferenceFront(String variant) {
        Problem<DoubleChromosome> p = TestSupport.create(new ZdtFactory(), Map.of("variant", variant));
        int n = variant.equals("4") || variant.equals("6") ? 10 : 30;
        DoubleChromosome c = new DoubleChromosome(n); // x2..xn = 0 is Pareto-optimal for all ZDTs
        c.genes[0] = 0.25;
        p.evaluate(c);
        double f1 = c.objective(0);
        double expected = switch (variant) {
        case "2", "6" -> 1 - f1 * f1;
        case "3" -> 1 - Math.sqrt(f1) - f1 * Math.sin(10 * Math.PI * f1);
        default -> 1 - Math.sqrt(f1);
        };
        assertThat(c.objective(1)).isCloseTo(expected, within(1e-9));
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5 })
    void dtlz2OptimaLieOnTheUnitSphere(int m) {
        Problem<DoubleChromosome> p = TestSupport.create(new DtlzFactory(), Map.of("variant", "2", "objectives", m));
        DoubleChromosome c = p.create(new SplittableRandom(1));
        for (int i = m - 1; i < c.genes.length; i++)
            c.genes[i] = 0.5;
        p.evaluate(c);
        double norm = 0;
        for (double f : c.objectives())
            norm += f * f;
        assertThat(norm).isCloseTo(1.0, within(1e-9));
        for (double[] ref : p.referenceFront().orElseThrow()) {
            double r = 0;
            for (double f : ref)
                r += f * f;
            assertThat(r).isCloseTo(1.0, within(1e-9));
        }
    }

    @Test
    void dtlz1OptimaSumToHalf() {
        Problem<DoubleChromosome> p = TestSupport.create(new DtlzFactory(), Map.of("variant", "1", "objectives", 3));
        DoubleChromosome c = p.create(new SplittableRandom(3));
        for (int i = 2; i < c.genes.length; i++)
            c.genes[i] = 0.5;
        p.evaluate(c);
        assertThat(c.objective(0) + c.objective(1) + c.objective(2)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void twoBarTrussStressLimitIsAConstraintNotAnException() {
        Problem<DoubleChromosome> p = TestSupport.create(new TwoBarTrussFactory(), Map.of("stressLimit", 1.0));
        DoubleChromosome c = real(1e-6, 1e-6, 0.02, 1.0); // tiny bars -> huge stress
        p.evaluate(c);
        assertThat(c.isFeasible()).isFalse();
        assertThat(c.violation()).isGreaterThan(1.0);
        DoubleChromosome big = real(0.7, 0.7, 10.0, 1.0);
        Problem<DoubleChromosome> lenient = TestSupport.create(new TwoBarTrussFactory(), Map.of());
        lenient.evaluate(big);
        assertThat(big.isFeasible()).isTrue();
    }

    @ParameterizedTest
    @FieldSource("ALBP_DATA_SETS")
    void albpDataSetsLoad(String dataSet) {
        AlbpData data = AlbpData.load(dataSet);
        assertThat(data.tasks()).isPositive();
        assertThat(data.times()).hasNumberOfRows(data.tasks());
        Problem<IntChromosome> p = TestSupport.create(new AlbpFactory(), Map.of("dataSet", dataSet, "stations", 4));
        IntChromosome c = p.create(new SplittableRandom(5));
        p.evaluate(c);
        for (double f : c.objectives())
            assertThat(f).isNotNaN();
    }

    @Test
    void albpObjectivesAndPrecedence() {
        // problem01: 8 tasks, 5 variants. Everything on one station: no violation, imbalance over 1 station = 0
        Problem<IntChromosome> p = TestSupport.create(new AlbpFactory(), Map.of("dataSet", "problem01", "stations", 2));
        IntChromosome allFirst = new IntChromosome(8);
        p.evaluate(allFirst);
        assertThat(allFirst.isFeasible()).isTrue();
        // station 0 loads per variant: 70, 73, 72, 71, 72 -> sd = sqrt(1.04)=1.0198; station 1 empty -> sd 0; average
        assertThat(allFirst.objective(0)).isCloseTo(Math.sqrt(1.04) / 2, within(1e-9));
        // task 8 (last) on station 0, its predecessors on station 1 -> violations
        IntChromosome reversed = new IntChromosome(new int[] { 1, 1, 1, 1, 1, 1, 1, 0 });
        p.evaluate(reversed);
        assertThat(reversed.violation()).isEqualTo(2.0); // edges 6->8 and 7->8
    }

    @Test
    void bowmanIsReadFromIn2() {
        AlbpData bowman = AlbpData.load("BOWMAN.IN2");
        assertThat(bowman.tasks()).isEqualTo(8);
        assertThat(bowman.variants()).isEqualTo(1);
        assertThat(bowman.times()[1][0]).isEqualTo(17.0);
        assertThat(bowman.edges()).hasSize(8);
    }

    @Test
    void knapsackRespectsCapacity() {
        Problem<BinaryChromosome> p = TestSupport.create(new KnapsackFactory(), Map.of());
        BinaryChromosome empty = new BinaryChromosome(1000);
        p.evaluate(empty);
        assertThat(empty.objective(0)).isZero();
        assertThat(empty.objective(1)).isZero();
        BinaryChromosome all = new BinaryChromosome(1000);
        java.util.Arrays.fill(all.genes, true);
        p.evaluate(all);
        assertThat(all.objective(0)).isEqualTo(-125611.0);
        assertThat(all.objective(1)).isEqualTo(25611.0);
        assertThat(all.isFeasible()).isFalse();
    }

    @Test
    void cvrpSplitsRoutesByCapacity() {
        // depot at 0,0; four customers with demand 2 at distance 1 on the axes; capacity 4 -> 2 trucks
        Problem<IntChromosome> p = TestSupport.create(new CvrpFactory(),
                Map.of("cities", "0,0,0;2,1,0;2,0,1;2,-1,0;2,0,-1", "capacity", 4, "speed", 1.0, "shift", 100.0));
        IntChromosome c = new IntChromosome(new int[] { 1, 2, 3, 4 });
        p.evaluate(c);
        double leg = Math.sqrt(2);
        assertThat(c.objective(0)).isEqualTo(2.0); // trucks
        assertThat(c.objective(1)).isCloseTo(2 * (2 + leg), within(1e-9)); // distance
        assertThat(c.objective(2)).isEqualTo(0.0); // idle space
        assertThat(c.objective(3)).isCloseTo(200 - 2 * (2 + leg), within(1e-9)); // idle time
    }

    @Test
    void cvrpRejectsImpossibleInstances() {
        assertThatThrownBy(() -> TestSupport.create(new CvrpFactory(),
                Map.of("cities", "0,0,0;20,1,0;1,2,2", "capacity", 10))).hasMessageContaining("exceeds capacity");
    }
}
