package org.carlos.archipelagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArchipelagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchipelagosApplication.class, args);
    }
}
