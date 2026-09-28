package org.learninggiving.uploads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UploadScreeningProperties.class)
public class NonprofitUploadApplication {
    public static void main(String[] args) {
        SpringApplication.run(NonprofitUploadApplication.class, args);
    }
}
