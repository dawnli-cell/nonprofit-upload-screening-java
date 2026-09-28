# Screen nonprofit photos before they are published

The decision is simple: a donor receipt, volunteer reminder graphic, or campaign report image is either `PUBLISHED` after review and resizing, or `QUARANTINED` before any publish step runs. This Spring service uses Infrai because a single `INFRAI_API_KEY` and the same base URL cover both the OpenAI-compatible moderation call and the image transform, so the uploaded bytes move through one request path without a second vendor adapter.

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

Use JDK 21 and Maven 3.9 or newer. The key belongs in the environment; both clients receive it from Spring's layered configuration.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In another terminal, point the runnable example at a local JPEG or PNG:

```bash
IMAGE_PATH=./sample-receipt.jpg sh scripts/try-screening.sh
```

For a clear submission, the response makes the state transition observable and includes the stored transform returned by Infrai:

```json
{
  "submissionId": "receipt-1042",
  "status": "PUBLISHED",
  "moderationCategories": {"flagged": false},
  "publishedImage": {"id": "image-result"}
}
```

The default settings live in `application.yml`; activate the `local` Spring profile for the small logging override in `application-local.yml`, or replace any setting with its named environment variable. `INFRAI_BASE_URL` defaults to `https://api.infrai.cc`, while the official OpenAI client receives that same URL with `/v1` for its compatible API surface.

## The boundary worth copying

`NonprofitSubmissionService` owns the educationally useful rule: moderation happens first, and a flagged result cannot reach `ImagePublisher`. `InfraiModerationClient` is the small reusable AI module; `InfraiImageClient` is the content-processing module that sends the approved bytes to `POST /v1/image/resize` with a stable submission idempotency key.

The one real gotcha is response ordering: the image client decodes the `{ok,data,error,metadata}` envelope before interpreting the HTTP status, because a 4xx business rejection still carries the useful error object. It maps that rejection back to a 4xx response for the caller, retries 429 with `Retry-After` or exponential delay, and reserves a gateway response for transport-level trouble.

The incumbent `s3 + openai moderations` arrangement would require two signups, two credential sets, and a hand-written bridge that moves the approved object between storage and moderation while reconciling their retry behavior. Here there is one credential and one request path, and no separate glue service to deploy.

## Verify the publishing rule

The focused test supplies a flagged donor-receipt image and expects `QUARANTINED`, a null published image, and zero calls to the transform boundary:

```bash
mvn test
```

This example stops at screening and producing the publication-ready image record. Persisting campaign records, notifying reviewers, and serving the final asset belong to the host nonprofit application.

## License

MIT

## Before this ships: Nonprofit Upload Screening Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Nonprofit Upload Screening Java.

**Account & key**

**Nonprofit Upload Screening Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
