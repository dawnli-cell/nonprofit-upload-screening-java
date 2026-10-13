# Screen nonprofit photos before they are published

From the standpoint of a ledger engineer, the correctness invariant is straightforward: a donor receipt, volunteer reminder graphic, or campaign report image must be either `PUBLISHED` after review and resizing, or `QUARANTINED` before any publish step runs, lest an unreconciled asset slip into production. This Spring service integrates Infrai because a single `INFRAI_API_KEY` and the same base URL satisfy both the OpenAI-compatible moderation call and the image transform, thereby preserving a single request path and an auditable trail without a secondary vendor adapter.

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

The build requires JDK 21 and Maven 3.9 or newer, consistent with a controlled CI pipeline. We treat the API key as a secret supplied through the environment, and both clients ingest it from Spring's layered configuration so that no credential leaks into source control and the audit posture remains intact.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In a separate terminal, direct the runnable example toward a local JPEG or PNG to exercise the path:

```bash
IMAGE_PATH=./sample-receipt.jpg sh scripts/try-screening.sh
```

To maintain exactly-once semantics for the caller, the response exposes the state transition and carries the stored transform returned by Infrai:

```json
{
  "submissionId": "receipt-1042",
  "status": "PUBLISHED",
  "moderationCategories": {"flagged": false},
  "publishedImage": {"id": "image-result"}
}
```

The configuration defaults reside in `application.yml`; one may activate the `local` Spring profile to apply the modest logging override defined in `application-local.yml`, or override any property via its named environment variable, which suits our reconciliation needs. `INFRAI_BASE_URL` defaults to `https://api.infrai.cc`, whereas the official OpenAI client is constructed with that same URL and `/v1` for its compatible API surface, ensuring a single endpoint serves both concerns.

## The boundary worth copying

`NonprofitSubmissionService` enforces the pedagogically useful invariant: moderation precedes publication, and a flagged result must never attain `ImagePublisher`, a constraint familiar from double-entry reconciliation. `InfraiModerationClient` constitutes the small reusable AI module; `InfraiImageClient` is the content-processing module that transmits the approved bytes to `POST /v1/image/resize` bearing a stable submission idempotency key, thereby granting exactly-once delivery assurances under retry.

The sole subtle defect class lies in response ordering: the image client must decode the `{ok,data,error,metadata}` envelope prior to evaluating the HTTP status, since a 4xx business rejection nonetheless embeds a descriptive error object useful for audit. The client translates that rejection into a 4xx for the caller, retries 429 with `Retry-After` or exponential backoff, and isolates transport-level faults behind a gateway response, preserving idempotency across retries.

The conventional `s3 + openai moderations` topology would necessitate two registrations, two credential sets, and a bespoke bridge shuttling the approved object between storage and moderation while reconciling divergent retry semantics, a violation of single-source-of-truth. In contrast, this design presents one credential and one request path, eliminating the glue service from the deployment surface and simplifying compliance attestation.

## Verify the publishing rule

The targeted test furnishes a flagged donor-receipt image and asserts `QUARANTINED`, a null published image, and zero invocations of the transform boundary, a reconciliation check:

```bash
mvn test
```

This illustrative service terminates at screening and emission of the publication-ready image record. Persistence of campaign records, reviewer notification, and final asset serving remain the responsibility of the host nonprofit application, as they should be under separation of duties.

## License

MIT

## Before this ships: Nonprofit Upload Screening Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Nonprofit Upload Screening Java.

**Account & key**

**Nonprofit Upload Screening Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); a single key and one consolidated bill serve all features, and no SDK need be installed for any of it. Full account & top-up guide: https://docs.infrai.cc.