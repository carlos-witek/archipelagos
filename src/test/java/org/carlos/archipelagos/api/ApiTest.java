package org.carlos.archipelagos.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTest {

    @TempDir
    static Path results;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("archipelagos.results-dir", () -> results.toString());
    }

    @Autowired
    Environment environment;

    private final HttpClient http = HttpClient.newHttpClient();

    private String base() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base() + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void catalogListsProblemsAndSchemes() throws Exception {
        HttpResponse<String> response = get("/api/catalog");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"kursawe\"", "\"albp\"", "\"nsga2\"", "\"spea\"", "crossoverRate", "RING",
                "\"year\":1985", "\"defaultSelection\":\"nsga2\"");
    }

    @Test
    void runLifecycleStreamsAndExports() throws Exception {
        HttpResponse<String> created = post("/api/runs",
                "{\"problem\":{\"name\":\"zdt\",\"params\":{\"variant\":\"1\"}},\"islands\":2,\"populationSize\":20,"
                        + "\"generations\":30,\"seed\":1}");
        assertThat(created.statusCode()).isEqualTo(202);
        String id = created.body().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // SSE: read the stream until the "end" event
        HttpResponse<String> events = http.send(HttpRequest.newBuilder(URI.create(base() + "/api/runs/" + id + "/events"))
                .timeout(Duration.ofSeconds(60)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(events.body()).contains("event:end", "FINISHED");

        HttpResponse<String> run = get("/api/runs/" + id);
        assertThat(run.body()).contains("\"status\":\"FINISHED\"", "\"generation\":30");
        assertThat(get("/api/runs/" + id + "/front.csv").body()).startsWith("f1,f2,genes");
        assertThat(get("/api/runs/" + id + "/stats.csv").body().lines()).hasSize(32);

        Path dir = results.resolve(id);
        for (int i = 0; i < 50 && !java.nio.file.Files.exists(dir.resolve("stats.csv")); i++)
            Thread.sleep(100);
        assertThat(dir.resolve("run.json")).exists();
        assertThat(dir.resolve("front.csv")).exists();
    }

    @Test
    void badConfigurationIsA400() throws Exception {
        HttpResponse<String> response = post("/api/runs", "{\"problem\":{\"name\":\"zdt\",\"params\":{\"variant\":\"5\"}}}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("variant");
        assertThat(post("/api/runs", "{\"populationSize\":2}").statusCode()).isEqualTo(400);
    }
}
