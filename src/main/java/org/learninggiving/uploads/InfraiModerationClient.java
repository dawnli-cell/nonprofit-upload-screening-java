package org.learninggiving.uploads;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationImageUrlInput;
import com.openai.models.moderations.ModerationMultiModalInput;
import com.openai.models.moderations.ModerationTextInput;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public final class InfraiModerationClient implements ContentModerator {
    private final OpenAIClient client;
    private final String model;

    public InfraiModerationClient(UploadScreeningProperties properties) {
        String baseUrl = properties.baseUrl() + "/v1";
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl(baseUrl)
                .build();
        this.model = properties.moderationModel();
    }

    @Override
    public ModerationVerdict review(byte[] image, String mediaType, String caption) {
        String dataUri = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        var response = client.moderations().create(ModerationCreateParams.builder()
                .model(model)
                .inputOfModerationMultiModalArray(List.of(
                        ModerationMultiModalInput.ofText(ModerationTextInput.builder()
                                .text(caption)
                                .build()),
                        ModerationMultiModalInput.ofImageUrl(ModerationImageUrlInput.builder()
                                .imageUrl(ModerationImageUrlInput.ImageUrl.builder()
                                        .url(dataUri)
                                        .build())
                                .build())))
                .build());
        var result = response.results().getFirst();
        Map<String, Boolean> categories = new LinkedHashMap<>();
        categories.put("flagged", result.flagged());
        return new ModerationVerdict(result.flagged(), categories);
    }
}
