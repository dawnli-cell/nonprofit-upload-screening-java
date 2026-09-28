package org.learninggiving.uploads;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public final class NonprofitSubmissionController {
    private final NonprofitSubmissionService submissions;

    public NonprofitSubmissionController(NonprofitSubmissionService submissions) {
        this.submissions = submissions;
    }

    @PostMapping("/submissions/screen")
    public SubmissionDecision screen(
            @RequestParam String submissionId,
            @RequestParam String caption,
            @RequestParam MultipartFile image) throws IOException {
        String contentType = image.getContentType() == null ? "application/octet-stream" : image.getContentType();
        return submissions.screen(submissionId, image.getBytes(), contentType,
                image.getOriginalFilename() == null ? "submission-image" : image.getOriginalFilename(), caption);
    }

    @ExceptionHandler(InfraiRequestException.class)
    public ResponseEntity<Map<String, Object>> infraiError(InfraiRequestException error) {
        int clientStatus = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(HttpStatus.valueOf(clientStatus))
                .body(Map.of("ok", false, "code", error.code(), "message", error.getMessage()));
    }
}
