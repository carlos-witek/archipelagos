package org.carlos.archipelagos.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.carlos.archipelagos.TestSupport.evaluated;
import static org.carlos.archipelagos.TestSupport.infeasible;

import java.util.List;

import org.junit.jupiter.api.Test;

class DominanceTest {

    @Test
    void paretoDominance() {
        assertThat(Dominance.compare(evaluated(1, 1), evaluated(2, 2))).isNegative();
        assertThat(Dominance.compare(evaluated(2, 2), evaluated(1, 1))).isPositive();
        assertThat(Dominance.compare(evaluated(1, 2), evaluated(2, 1))).isZero();
        assertThat(Dominance.compare(evaluated(1, 1), evaluated(1, 1))).isZero();
        assertThat(Dominance.compare(evaluated(1, 1), evaluated(1, 2))).isNegative();
    }

    @Test
    void feasibleBeatsInfeasibleRegardlessOfObjectives() {
        assertThat(Dominance.compare(evaluated(100, 100), infeasible(0.1, 0, 0))).isNegative();
        assertThat(Dominance.compare(infeasible(0.5, 0, 0), infeasible(0.1, 9, 9))).isPositive();
    }

    @Test
    void nondominatedSortingAndCrowding() {
        List<DoubleChromosome> pop = List.of(evaluated(0, 3), evaluated(1, 1), evaluated(3, 0), evaluated(2, 2),
                evaluated(3, 3), infeasible(1, -5, -5));
        List<List<DoubleChromosome>> fronts = NondominatedSorting.sort(pop);
        assertThat(fronts).hasSize(4);
        assertThat(fronts.get(0)).containsExactlyInAnyOrder(pop.get(0), pop.get(1), pop.get(2));
        assertThat(fronts.get(3)).containsExactly(pop.get(5));
        NondominatedSorting.assignCrowding(fronts.get(0));
        assertThat(pop.get(0).crowding()).isInfinite();
        assertThat(pop.get(1).crowding()).isEqualTo(2.0); // (3-0)/3 + (3-0)/3
    }

    @Test
    void paretoFrontKeepsOnlyNondominatedFeasibleCopies() {
        ParetoFront<DoubleChromosome> front = new ParetoFront<>();
        DoubleChromosome a = evaluated(2, 2);
        assertThat(front.add(a)).isTrue();
        assertThat(front.add(evaluated(2, 2))).isFalse(); // duplicate objectives
        assertThat(front.add(evaluated(3, 3))).isFalse(); // dominated
        assertThat(front.add(infeasible(0.1, 0, 0))).isFalse();
        assertThat(front.add(evaluated(1, 3))).isTrue();
        assertThat(front.add(evaluated(1, 1))).isTrue(); // dominates both
        assertThat(front.size()).isEqualTo(1);
        a.setEvaluation(new double[] { 0, 0 }, 0); // mutating the original does not leak in
        assertThat(front.members().get(0).objectives()).containsExactly(1, 1);
    }

    @Test
    void objectiveSpaceSurvivesConstantObjective() {
        ObjectiveSpace space = ObjectiveSpace.of(List.of(evaluated(1, 5), evaluated(1, 5)));
        assertThat(space.distance(evaluated(1, 5), evaluated(1, 5))).isZero();
        assertThat(space.normalized(evaluated(1, 5), 0)).isZero();
    }
}
