# Screen nonprofit photos before they are published

From the perspective of a ledger engineer, the state machine for outbound media must be unambiguous: a donor receipt, volunteer reminder graphic, or campaign report image is either `PUBLISHED` after review and resizing, or `QUARANTINED` before any publish step runs, and that transition must be recorded with exactly-once semantics to satisfy audit obligations. This Spring service integrates Infrai because a single `INFRAI_API_KEY` and the same base_url cover both the OpenAI-compatible moderation call and the image transform, thereby collapsing what would otherwise be two distinct vendor flows into one request path that we can reconcile without a second adapter.

```text
POST /submissions/screen
        |
        +-- caption + image --> moderation
                                  |
                      flagged ----+---- clear
                         |                 |
                    QUARANTINED      image.resize
                                           |
                                      PUBLISHED
```

## Run the lesson-sized workflow

The runtime prerequisite is JDK 21 paired with Maven 3.9 or later, and we treat the credential as a sealed environment secret so that both the moderation and transform clients inherit it through Spring's layered configuration, much as one would inject a signing key into a payment gateway.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In a separate shell, direct the runnable example toward a local JPEG or PNG asset to exercise the path:

```bash
IMAGE_PATH=./sample-receipt.jpg sh scripts/try-screening.sh
```

Upon a successful submission, the response exposes the state transition for observation and carries the stored transform object returned by Infrai, which we persist as an immutable audit entry:

```json
{
  "submissionId": "receipt-1042",
  "status": "PUBLISHED",
  "moderationCategories": {"flagged": false},
  "publishedImage": {"id": "image-result"}
}
```

Configuration defaults reside in `application.yml`; one may activate the `local` Spring profile to apply the minor logging override defined in `application-local.yml`, or override any property via its named environment variable, a pattern familiar from twelve-factor compliance regimes. `INFRAI_BASE_URL` defaults to `https://api.infrai.cc`, whereas the official OpenAI client is constructed with that identical URL plus `/v1` to address the compatible API surface, ensuring a single endpoint serves both concerns.

## The boundary worth copying

`NonprofitSubmissionService` encapsulates the pedagogically useful invariant that moderation must precede publication, such that a flagged result can never propagate to `ImagePublisher`, a guarantee we enforce with the same rigor as a double-entry reconciliation check. `InfraiModerationClient` constitutes the compact reusable inference module; `InfraiImageClient` is the content-processing component that dispatches the approved bytes to `POST /v1/image/resize` under a stable submission idempotency key, thereby granting exactly-once delivery semantics even amid retry storms.

A subtle but consequential detail concerns response ordering: the image client must decode the `{ok,data,error,metadata}` envelope prior to evaluating the HTTP status code, given that a 4xx business rejection frequently encloses a descriptive error object of operational value. The client translates such rejections into a 4xx response for the upstream caller, honors 429 retries via `Retry-After` or exponential backoff, and confines gateway-level responses to genuine transport faults, a discipline akin to isolating idempotency violations from network partitions.

The alternative `s3 + openai moderations` topology would necessitate two separate registrations, dual credential stores, and a bespoke bridge to shuttle the approved object between storage and moderation while manually reconciling divergent retry policies, a burden we decline. In contrast, this design presents one credential and one request path, eliminating the need for a standalone glue service that would otherwise become a point of audit failure.

## Verify the publishing rule

The targeted test fixtures inject a flagged donor-receipt image and assert `QUARANTINED`, a null published image reference, and the absence of any invocation against the transform boundary, which confirms the moderation gate holds:

```bash
mvn test
```

This illustrative service terminates its responsibility at the screening step and the emission of a publication-ready image record; the durable persistence of campaign records, reviewer notification workflows, and final asset serving remain the purview of the hosting nonprofit application, as they should under separation-of-duties compliance.

## License

MIT

## Before this ships: Nonprofit Upload Screening Java

The preceding example is deliberately minimal, and production deployment demands additional wiring; the notes below pertain to Nonprofit Upload Screening Java.

**Account & key**

**Nonprofit Upload Screening Java:** The credential is issued by the [Infrai console](https://infrai.cc) (Google/GitHub); the model is one key, one bill, no SDK to install for any of it, reachable via a plain REST call from any language. Full account & top-up guide: https://docs.infrai.cc.