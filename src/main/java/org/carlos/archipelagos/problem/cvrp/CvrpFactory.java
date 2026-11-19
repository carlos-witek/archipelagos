package org.carlos.archipelagos.problem.cvrp;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import org.carlos.archipelagos.core.IntChromosome;
import org.carlos.archipelagos.operator.PermutationOperators;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.param.Params;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemFactory;
import org.carlos.archipelagos.problem.Variation;
import org.carlos.archipelagos.problem.VariationParams;
import org.springframework.stereotype.Component;

/**
 * Capacitated vehicle routing with a shift limit (thesis problem). The genotype is a permutation
 * of customers; it is split greedily into routes: a truck leaves the depot and visits customers in
 * order until the next one would exceed its capacity or its shift (including the way back).
 * <p>
 * Objectives: trucks, total distance, idle space (unused capacity), idle time (unused shift).
 * <p>
 * The 2008 version never ran: the route list was never initialised, cities were never stored,
 * distance was never accumulated, the truck shift was never set (so every visit "overflowed" and
 * the decoder looped forever), city x/y were parsed from the demand/x columns and idle time used an
 * active time that stayed 0.
 */
@Component
public class CvrpFactory implements ProblemFactory {

    @Override
    public String name() {
        return "cvrp";
    }

    @Override
    public String description() {
        return "Capacitated vehicle routing with shift limit; permutation genotype, greedy route split.";
    }

    @Override
    public List<ParamSpec> params() {
        return VariationParams.with(List.of(
                ParamSpec.string("cities", "'demand,x,y;...' first entry is the depot (empty = generated instance)", ""),
                ParamSpec.integer("generatedCustomers", "customers of the generated instance", 30, 2, 10000),
                ParamSpec.integer("capacity", "truck capacity", 10, 1, 1000000),
                ParamSpec.decimal("speed", "truck average speed", 10.0, 1e-9, 1e9),
                ParamSpec.decimal("shift", "truck shift length (time)", 100.0, 1e-9, 1e12),
                ParamSpec.decimal("inversionShare", "share of mutations that are inversions (rest are swaps)", 0.5, 0.0, 1.0)));
    }

    record City(int demand, double x, double y) {
    }

    @Override
    public Problem<?> create(Params p) {
        List<City> cities = parseOrGenerate(p.getString("cities"), p.getInt("generatedCustomers"));
        int capacity = p.getInt("capacity");
        double speed = p.getDouble("speed");
        double shift = p.getDouble("shift");
        City depot = cities.get(0);
        for (int i = 1; i < cities.size(); i++) {
            City c = cities.get(i);
            if (c.demand() > capacity)
                throw new IllegalArgumentException("Customer " + i + " demand " + c.demand() + " exceeds capacity " + capacity);
            if (2 * dist(depot, c) / speed > shift)
                throw new IllegalArgumentException("Customer " + i + " cannot be reached and returned from within one shift");
        }
        return new CvrpProblem(cities, capacity, speed, shift, p.getDouble(VariationParams.CROSSOVER_RATE),
                VariationParams.mutationRate(p, cities.size() - 1), p.getDouble("inversionShare"));
    }

    static List<City> parseOrGenerate(String data, int generated) {
        List<City> cities = new ArrayList<>();
        if (data == null || data.isBlank()) {
            SplittableRandom rng = new SplittableRandom(2008);
            cities.add(new City(0, 50, 50));
            for (int i = 0; i < generated; i++)
                cities.add(new City(1 + rng.nextInt(4), rng.nextDouble(100), rng.nextDouble(100)));
            return cities;
        }
        for (String entry : data.split(";")) {
            if (entry.isBlank())
                continue;
            String[] values = entry.trim().split(",");
            if (values.length != 3)
                throw new IllegalArgumentException("City '" + entry + "' must be 'demand,x,y'");
            cities.add(new City(Integer.parseInt(values[0].trim()), Double.parseDouble(values[1].trim()),
                    Double.parseDouble(values[2].trim())));
        }
        if (cities.size() < 3)
            throw new IllegalArgumentException("Need a depot and at least two customers");
        return cities;
    }

    static double dist(City a, City b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    static final class CvrpProblem implements Problem<IntChromosome> {

        private final List<City> cities;
        private final int capacity;
        private final double speed;
        private final double shift;
        private final int[] customers;
        private final Variation<IntChromosome> variation;

        CvrpProblem(List<City> cities, int capacity, double speed, double shift, double crossoverRate,
                double mutationRate, double inversionShare) {
            this.cities = List.copyOf(cities);
            this.capacity = capacity;
            this.speed = speed;
            this.shift = shift;
            this.customers = new int[cities.size() - 1];
            for (int i = 0; i < customers.length; i++)
                customers[i] = i + 1;
            // per-chromosome mutation chance scaled so that on average mutationRate * n genes move
            double chromosomeRate = Math.min(1.0, mutationRate * customers.length);
            this.variation = new Variation<>() {
                @Override
                public double crossoverRate() {
                    return crossoverRate;
                }

                @Override
                public void crossover(IntChromosome a, IntChromosome b, RandomGenerator rng) {
                    PermutationOperators.pmx(a.genes, b.genes, rng);
                }

                @Override
                public void mutate(IntChromosome c, RandomGenerator rng) {
                    if (rng.nextDouble() >= chromosomeRate)
                        return;
                    if (rng.nextDouble() < inversionShare)
                        PermutationOperators.inversion(c.genes, 0.66, rng);
                    else
                        PermutationOperators.swap(c.genes, rng);
                }
            };
        }

        @Override
        public String name() {
            return "cvrp";
        }

        @Override
        public List<String> objectives() {
            return List.of("trucks", "distance", "idleSpace", "idleTime");
        }

        @Override
        public IntChromosome create(RandomGenerator rng) {
            return new IntChromosome(PermutationOperators.shuffled(customers, rng));
        }

        @Override
        public void evaluate(IntChromosome c) {
            City depot = cities.get(0);
            int trucks = 1;
            double distance = 0.0;
            double idleSpace = 0.0;
            double idleTime = 0.0;
            City position = depot;
            int load = 0;
            double time = 0.0;
            for (int index : c.genes) {
                City next = cities.get(index);
                double leg = dist(position, next);
                double back = dist(next, depot);
                boolean fits = load + next.demand() <= capacity && time + (leg + back) / speed <= shift;
                if (!fits) {
                    double home = dist(position, depot);
                    distance += home;
                    time += home / speed;
                    idleSpace += capacity - load;
                    idleTime += shift - time;
                    trucks++;
                    position = depot;
                    load = 0;
                    time = 0.0;
                    leg = dist(depot, next);
                }
                distance += leg;
                time += leg / speed;
                load += next.demand();
                position = next;
            }
            double home = dist(position, depot);
            distance += home;
            time += home / speed;
            idleSpace += capacity - load;
            idleTime += shift - time;
            c.setEvaluation(new double[] { trucks, distance, idleSpace, idleTime }, 0.0);
        }

        @Override
        public Variation<IntChromosome> variation() {
            return variation;
        }
    }
}
