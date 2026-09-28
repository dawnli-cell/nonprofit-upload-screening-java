package org.learninggiving.uploads;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("infrai")
public record UploadScreeningProperties(
        String baseUrl,
        String apiKey,
        String moderationModel,
        int publishWidth,
        int publishHeight) {
}
