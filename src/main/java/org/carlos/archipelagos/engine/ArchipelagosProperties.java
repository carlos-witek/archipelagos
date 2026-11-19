package org.carlos.archipelagos.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("archipelagos")
public record ArchipelagosProperties(String resultsDir, Integer maxConcurrentRuns) {

    public ArchipelagosProperties {
        if (resultsDir == null || resultsDir.isBlank())
            resultsDir = "results";
        if (maxConcurrentRuns == null || maxConcurrentRuns < 1)
            maxConcurrentRuns = 4;
    }
}
