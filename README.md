# Archipelagos 2

A port of the 2008 master's thesis project *Archipelagos* (a distributed island-model multi-objective genetic algorithm framework) to a single **Spring Boot 4.1 / Java 21** application.

- **One process, one Gradle module.** There is no JBoss, no WAR, no RMI and no mobile code.
- **Islands run in parallel** on virtual threads. Migration happens between generations.
- **Runs are reproducible from their seed.**
- **REST API + Server-Sent Events**, with a small web page at `/` that builds its forms from the parameter catalog.
- **All problems and selection schemes from the thesis**, with the defects from `ARCHIPELAGOS_REVIEW.md` (in the original workspace on the backup drive) fixed. NSGA-II, ZDT and DTLZ are added.

## Build and run

```bash
./gradlew test
```

```bash
./gradlew bootRun
```

Then open http://localhost:8080.

If the project sits on an exFAT/FAT volume, macOS writes `._*` files next to every file, and they break Gradle's clean-up of its output folders. In that case build elsewhere with `-PbuildDir=/tmp/archipelagos-build`.

Finished runs are written to `results/<run-id>/` as `run.json`, `front.csv` and `stats.csv`. Change the location with `archipelagos.results-dir`.

## API

| Method | Path | |
|---|---|---|
| GET | `/api/catalog` | Problems, selection schemes, topologies and policies, each with parameter specs (type, default, range, choices) |
| POST | `/api/problems/{name}/reference` | Reference front of a problem with the given params |
| POST | `/api/runs` | Start a run (body below). Returns 202 plus the run |
| GET | `/api/runs`, `/api/runs/{id}` | List runs / one run including its current front |
| GET | `/api/runs/{id}/events` | SSE: `generation` events (throttled to 5/s), then `end` |
| GET | `/api/runs/{id}/stats`, `/stats.csv`, `/front.csv`, `/reference` | History, exports |
| POST | `/api/runs/{id}/stop` · DELETE `/api/runs/{id}` | Stop / forget |

```json
{
  "problem":   { "name": "albp", "params": { "dataSet": "problem03", "stations": 6 } },
  "selection": { "name": "spea", "params": { "externalSize": 8 } },
  "islands": 4,
  "populationSize": 100,
  "offspring": null,
  "generations": 300,
  "restartInterval": 0,
  "migration": { "topology": "RING", "interval": 5, "rate": 0.1,
                 "emigrants": "BEST", "immigrants": "REPLACE_WORST", "copy": true },
  "referencePoint": null,
  "seed": 2008
}
```

Everything except `problem.name` is optional. `GET /api/catalog` lists every parameter and its default.

## Layout and where the 2008 code went

```
org.carlos.archipelagos
  core/       Chromosome (+Double/Int/Binary), Dominance, ParetoFront, NondominatedSorting, ObjectiveSpace, Simplex
  param/      ParamSpec, Params                 <- @Input/@Default/@Values annotations
  operator/   Real/Int/Permutation/Binary operators
  problem/    Problem, Variation, ProblemFactory, ProblemCatalog, ReferenceFronts
    real/     Schaffer, Kursawe, ZDT, DTLZ, two-bar truss
    albp/ knapsack/ cvrp/
  selection/  Vega, Moga, Nsga, Npga, Spea, WbgaVe, WbgaSf, RandomObjectiveSelection, Nsga2, SelectionCatalog
  migration/  Topology, EmigrantPolicy, ImmigrantPolicy
  metrics/    Metrics (HV, GD, IGD, IGD+, spacing, max spread, MFE)
  engine/     Island, Archipelago, Run, RunService, ResultExporter
  api/        REST controllers, SSE, error mapping
```

| 2008 (`org.carlos.archipelagos.commons` unless noted) | Now |
|---|---|
| `commons/genetic/Chromosome`, `impl/Chromosome*` | `core/Chromosome`, `DoubleChromosome`, `IntChromosome`, `BinaryChromosome` |
| `commons/genetic/ChromosomeRelation` | `core/Dominance` |
| `commons/genetic/Front` | `core/ParetoFront` |
| `commons/genetic/Pool`, `Population` | plain `List<C>` inside `engine/Island` |
| `commons/genetic/Objective`, `Property` | objective names `List<String>`, objective values `double[]`; WBGA group → `Chromosome.group` |
| `commons/problem/Problem`, `ProblemContext`, `ProblemProvider` | `problem/Problem`, `ProblemFactory`, `RunRequest` |
| `strategies/IStrategy*` + `annotations/*` | `Problem.create/evaluate`, `Variation`, `param/ParamSpec` |
| `strategies/impl/initialization/*`, `recombination/*` | `operator/*`, `problem/real/RealProblem` |
| `strategies/impl/restart/RestartAfterNGeneration` | `RunRequest.restartInterval`, `generations` |
| `commons/selection/impl/Selection*` | `selection/*` |
| `commons/migration/impl/Migration*` | `migration/EmigrantPolicy`, `ImmigrantPolicy`, `Topology` |
| `problem/impl/schaffer`, `kursawe`, `twobartruss` | `problem/real/*Factory` |
| `problem/impl/albp` (+ `data/`) | `problem/albp/*`, `resources/data/albp/` |
| `problem/impl/knapsack` (+ `data/`) | `problem/knapsack/KnapsackFactory`, `resources/data/knapsack/` |
| `problem/impl/cvrp` | `problem/cvrp/CvrpFactory` |
| `archipelagos-client/.../tools/metrics/calculators/*` | `metrics/Metrics` |
| `archipelagos-client/results/kursawe/front-complete.xml` | `resources/data/reference/kursawe.csv` |
| `.deme`, `.enviroment`, `archipelagos_server.jar` (2006 RMI) | `engine/Island`, `Archipelago`, `Run`, `RunService` |
| `archipelagos-client` SWT UI, `org.carlos.archipeagos.rcp` | `api/*`, `static/index.html` |

**Not ported:**
- `problem/impl/nnetwork`: its fitness was a stub that returned 0.
- `org.carlos.iced_2078`: unrelated to the thesis.
- The brute-force solver (`archipelagos-problems`): replaced by analytical or bundled reference fronts.
- Beacon/Quartz, JNDI/JMX plumbing, SWT wrappers, XML printers and translators: replaced by CSV/JSON export.

## Deviations from the thesis code

The port fixes every defect from the review and reformulates what never worked. This means **results won't match numbers produced by the 2008 code**.

**Algorithms**
- **Constraints:** use Deb's constrained domination (a feasible solution beats an infeasible one; between two infeasible ones the smaller violation wins). This replaces the multiplicative penalty in [0,1). The proportional schemes give infeasible individuals a tiny fitness that shrinks as the violation grows.
- **Evaluation:** recomputed from scratch for every child, so offspring no longer inherit stale penalties.
- **Sharing:** distances are taken in normalised objective space and guard against constant objectives (2008 produced NaN here). The default niche radius is now `0.1`. At 2008's `0.01`, sharing barely did anything; pass `sigmaShare: 0.01` to get the old value.
- **VEGA:** each objective selects from the whole population (2008 dealt the population into fixed groups). The worst individual keeps a small chance.
- **NSGA:** proper layered non-dominated fronts (2008 used MOGA's dominator count) with ranks in order (2008 used `HashMap` order).
- **NPGA:** a candidate *dominated by* the comparison set loses (2008 used the reverse test).
- **SPEA:**
  - Dominated members are removed from the external set.
  - The external set is truncated by average-linkage clustering.
  - Mating selection is binary tournaments over P + P'.
  - It works for populations under 10.
- **WBGA:** weights apply to normalised objectives. Empty vectors mean evenly spaced ones.
- **Roulette:** uses binary search and falls back to uniform selection when the total fitness is 0 or not finite.
- **Pairing:** offspring are shuffled before pairing, so schemes that fill the mating pool in blocks still mix across blocks.
- **NSGA-II:** added as the default scheme. It is elitist and produces a full population of offspring. Classic schemes default to `populationSize / 2` offspring.

**Migration and restarts**
- **Emigrant draws** are without replacement (2008 could send the same individual twice).
- **Small rates:** the rate rounds to at least one migrant (2008 crashed when `rate × size < 1`).
- **Single island:** `RANDOM` topology works with one island.
- **Interval:** migration happens every `interval` generations (2008 migrated every generation).
- **Immigrant policy** is configurable: `ADD` is the 2008 behaviour, `REPLACE_WORST` is the default.
- **One level of islands** replaces environment → deme → stock.
- **Lock-step islands:** all islands advance one generation at a time in parallel. 2008 ran the stocks one after another and slept 1 s after each.
- **Restarts** happen every `restartInterval` generations (2008 restarted on *every* generation once the threshold was reached). The archive is kept across restarts.

**Operators**
- **Real-valued:** SBX + polynomial mutation are the default. `operators: "averaging"` selects the 2008 crossover, now clamped to the bounds, plus a range-relative Gaussian mutation (2008's `g += 0.1·g·N(0,1)` could never move a gene that was 0).
- **Permutations:** proper PMX + swap/inversion (2008's crossover crashed on the first call).
- **Mutation rates** are per gene, with 1/n as the default (2008 used one gene per chromosome with a per-chromosome probability).

**Problems**
- **Schaffer, Kursawe:** unchanged formulas. Kursawe's reference is the 2008 brute-force front.
- **Two-bar truss:**
  - The stress limit is a constraint (2008 threw once a penalty reached 1.0).
  - Variables are bounded and strictly positive.
- **ALBP:**
  - The problem can be instantiated (2008 cast its fitness to `CVRPFitness`).
  - `standardDeviation` is the real per-station standard deviation over the variants, averaged over the stations. 2008 summed the absolute deviations.
  - Precedence violations are a constraint.
  - Bowman is stored as IN2 (its 2008 files mixed two formats). Jaeschke and Mansoor are now loadable.
  - `jackson` is selectable (2008's data-set list didn't include it).
- **Knapsack:** reformulated (2008 never ran). It's now a bit string: maximise value (reported as `negValue`), minimise weight, with the capacity from the data file as a constraint. The `501 499` line in the data files holds item counts, not capacities.
- **CVRP:** rewritten (2008 never ran). It uses a greedy route split by capacity and shift, with objectives trucks, distance, idle space and idle time. Coordinates come from the correct columns. With no `cities` given, a seeded 30-customer instance is generated.
- **Added:** ZDT1–4 and 6, DTLZ1–2.

**Metrics**
- **GD** = sqrt(Σd²)/n (2008: sqrt(Σd)/n).
- **Spacing** is Schott's: nearest neighbour within the same front (2008 measured distance to the reference front).
- **Added:** IGD and IGD+.
- **Hypervolume reference point:** taken from the request, or the reference front's nadir + 10 % of its range. HV is reported for up to 4 objectives.

## Known limitations

- **Unbounded fronts:** the collective front and the island archives grow without limit, so long runs on continuous problems produce thousands of points and metrics slow down. A bounded (crowding-truncated) archive would be the next step.
- **Runs live in memory.** Only the export in `results/` survives a restart.
- **The web page** plots the first two objectives.

## Licence

Apache License 2.0, see [LICENSE](LICENSE).
