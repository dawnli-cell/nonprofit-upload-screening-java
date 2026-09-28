package org.learninggiving.uploads;

import java.util.Map;

public record SubmissionDecision(
        String submissionId,
        Status status,
        Map<String, Boolean> moderationCategories,
        Object publishedImage) {

    public enum Status {
        PUBLISHED,
        QUARANTINED
    }

    static SubmissionDecision quarantined(String id, Map<String, Boolean> categories) {
        return new SubmissionDecision(id, Status.QUARANTINED, categories, null);
    }

    static SubmissionDecision published(String id, Map<String, Boolean> categories, Object image) {
        return new SubmissionDecision(id, Status.PUBLISHED, categories, image);
    }
}
