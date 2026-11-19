package org.carlos.archipelagos.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.carlos.archipelagos.migration.EmigrantPolicy;
import org.carlos.archipelagos.migration.ImmigrantPolicy;
import org.carlos.archipelagos.migration.Topology;
import org.carlos.archipelagos.param.ParamSpec;
import org.carlos.archipelagos.problem.Problem;
import org.carlos.archipelagos.problem.ProblemCatalog;
import org.carlos.archipelagos.selection.SelectionCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** What can be configured: problems, selection schemes and migration options, with parameter specs. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    /** @param year publication year (selection schemes only, otherwise null) */
    public record Item(String name, Integer year, String description, List<ParamSpec> params) {
    }

    /** @param defaultSelection scheme used when a run request names none */
    public record Catalog(List<Item> problems, List<Item> selections, String defaultSelection,
            List<Topology> topologies, List<EmigrantPolicy> emigrantPolicies, List<ImmigrantPolicy> immigrantPolicies) {
    }

    public record Reference(String problem, List<String> objectives, List<double[]> front) {
    }

    private final ProblemCatalog problems;
    private final SelectionCatalog selections;

    public CatalogController(ProblemCatalog problems, SelectionCatalog selections) {
        this.problems = problems;
        this.selections = selections;
    }

    @GetMapping("/catalog")
    public Catalog catalog() {
        return new Catalog(
                problems.factories().stream().map(f -> new Item(f.name(), null, f.description(), f.params())).toList(),
                selections.entries().stream().map(e -> new Item(e.name(), e.year(), e.description(), e.params()))
                        .toList(),
                SelectionCatalog.DEFAULT, Arrays.asList(Topology.values()), Arrays.asList(EmigrantPolicy.values()),
                Arrays.asList(ImmigrantPolicy.values()));
    }

    /** Reference front of a problem configured with the given parameters (empty if unknown). */
    @PostMapping("/problems/{name}/reference")
    public Reference reference(@PathVariable String name, @RequestBody(required = false) Map<String, Object> params) {
        Problem<?> problem = problems.create(name, params);
        return new Reference(problem.name(), problem.objectives(), problem.referenceFront().orElse(List.of()));
    }
}
