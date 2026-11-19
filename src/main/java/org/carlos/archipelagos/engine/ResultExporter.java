package org.carlos.archipelagos.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a finished run to {@code <results-dir>/<run-id>/}: {@code run.json} (config, seed,
 * status), {@code front.csv} and {@code stats.csv}. Replaces the 2008 XML printers and the
 * GNUPlot/XML translators: CSV opens in any plotting tool.
 */
@Component
public class ResultExporter {

    private final Path root;
    private final JsonMapper json;

    public ResultExporter(ArchipelagosProperties properties, JsonMapper json) {
        this.root = Path.of(properties.resultsDir());
        this.json = json;
    }

    public Path export(Run run) {
        Path dir = root.resolve(run.id().toString());
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("run.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(RunView.of(run, false)),
                    StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("front.csv"), frontCsv(run.objectives(), run.front()), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("stats.csv"), statsCsv(run.history()), StandardCharsets.UTF_8);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String frontCsv(List<String> objectives, List<FrontPoint> front) {
        StringBuilder csv = new StringBuilder(String.join(",", objectives)).append(",genes\n");
        for (FrontPoint point : front) {
            StringJoiner row = new StringJoiner(",");
            for (double v : point.objectives())
                row.add(Double.toString(v));
            row.add('"' + point.genes() + '"');
            csv.append(row).append('\n');
        }
        return csv.toString();
    }

    public static String statsCsv(List<GenerationStats> history) {
        StringBuilder csv = new StringBuilder(
                "generation,evaluations,frontSize,hypervolume,gd,igd,igdPlus,mfe,spacing,maximumSpread,elapsedMillis\n");
        for (GenerationStats s : history)
            csv.append(String.format(Locale.ROOT, "%d,%d,%d,%s,%s,%s,%s,%s,%s,%s,%d%n", s.generation(), s.evaluations(),
                    s.frontSize(), str(s.hypervolume()), str(s.generationalDistance()),
                    str(s.invertedGenerationalDistance()), str(s.invertedGenerationalDistancePlus()),
                    str(s.maximumFrontError()), str(s.spacing()), str(s.maximumSpread()), s.elapsedMillis()));
        return csv.toString();
    }

    private static String str(Double value) {
        return value == null ? "" : value.toString();
    }
}
