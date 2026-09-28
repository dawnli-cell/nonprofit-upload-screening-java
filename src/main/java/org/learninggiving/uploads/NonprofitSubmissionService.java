package org.learninggiving.uploads;

import org.springframework.stereotype.Service;

@Service
public final class NonprofitSubmissionService {
    private final ContentModerator moderator;
    private final ImagePublisher publisher;

    public NonprofitSubmissionService(ContentModerator moderator, ImagePublisher publisher) {
        this.moderator = moderator;
        this.publisher = publisher;
    }

    public SubmissionDecision screen(String submissionId, byte[] image, String mediaType,
                                     String filename, String caption) {
        ContentModerator.ModerationVerdict verdict = moderator.review(image, mediaType, caption);
        if (verdict.flagged()) {
            return SubmissionDecision.quarantined(submissionId, verdict.categories());
        }
        Object publishedImage = publisher.resizeForPublication(image, filename, "submission-" + submissionId);
        return SubmissionDecision.published(submissionId, verdict.categories(), publishedImage);
    }
}
