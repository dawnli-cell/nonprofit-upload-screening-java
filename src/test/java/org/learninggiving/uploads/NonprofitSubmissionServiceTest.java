package org.learninggiving.uploads;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class NonprofitSubmissionServiceTest {
    @Test
    void quarantinesFlaggedReceiptWithoutPublishingIt() {
        ContentModerator moderator = (image, mediaType, caption) ->
                new ContentModerator.ModerationVerdict(true, Map.of("flagged", true));
        AtomicBoolean publishCalled = new AtomicBoolean(false);
        ImagePublisher publisher = (image, filename, key) -> {
            publishCalled.set(true);
            return Map.of("id", "published-image");
        };

        SubmissionDecision decision = new NonprofitSubmissionService(moderator, publisher)
                .screen("receipt-1042", new byte[]{1, 2, 3}, "image/jpeg", "receipt.jpg",
                        "Receipt from the school supply drive");

        assertEquals(SubmissionDecision.Status.QUARANTINED, decision.status());
        assertNull(decision.publishedImage());
        assertFalse(publishCalled.get());
    }
}
