package org.learninggiving.uploads;

public interface ImagePublisher {
    Object resizeForPublication(byte[] image, String filename, String idempotencyKey);
}
