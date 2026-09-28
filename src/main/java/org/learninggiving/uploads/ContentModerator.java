package org.learninggiving.uploads;

import java.util.Map;

public interface ContentModerator {
    ModerationVerdict review(byte[] image, String mediaType, String caption);

    record ModerationVerdict(boolean flagged, Map<String, Boolean> categories) {
    }
}
