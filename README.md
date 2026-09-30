# Product Availability Service

A small REST microservice that exposes real-time product availability for a single SKU, built for Aritzia's (Senior) Developer, Backend case assessment.

## Tech stack

- Java 21
- Spring Boot 3.3 (Web, Validation, Cache, Actuator)
- Maven
- Caffeine (in-process cache)
- Bucket4j (in-process rate limiting)
- springdoc-openapi (Swagger UI / OpenAPI spec)
- JUnit 5, Mockito, AssertJ (tests)

## Architecture

```
Controller  -> validates nothing itself; delegates to service
Service     -> validates productId, applies business logic, cached
Validator   -> productId format rules (single source of truth)
Repository  -> in-memory data access (10 seeded SKUs)
```

Request flow: `RateLimitFilter` (servlet filter, runs before Spring MVC) -> `ProductAvailabilityController` -> `ProductAvailabilityService` (validates, then checks cache, then repository on a miss) -> `InMemoryProductRepository`.

## Setup instructions

Prerequisites: JDK 21, Maven 3.9+ (Docker only needed for the container path below).

```bash
git clone https://github.com/akhilkateja123/ProductAvailabilityService.git
cd ProductAvailabilityService
mvn -q -DskipTests compile   # fetch dependencies, verify it compiles
```

No config, database, or environment setup is required — the service seeds its own in-memory product data at startup.

## How to run

### Option A — Maven (no Docker)

```bash
mvn spring-boot:run
```

### Option B — Docker

```bash
docker build -t product-availability-service .
docker run -p 8080:8080 product-availability-service
```

Both start the service on `http://localhost:8080` (override the port with the `PORT` env var, e.g. `docker run -p 9090:9090 -e PORT=9090 product-availability-service`).

Run the test suite:

```bash
mvn test
```

## API

### `GET /availability/{productId}`

`productId` must be 1-20 numeric digits (surrounding whitespace is trimmed). Internal whitespace, letters, or symbols are rejected.

**200 OK**

```bash
curl http://localhost:8080/availability/10001
```
```json
{"productId":"10001","inStock":true,"availableQuantity":42,"lastUpdated":"2026-03-18T18:20:00Z"}
```

**200 OK, out of stock** (`quantity = 0`)

```bash
curl http://localhost:8080/availability/10003
```
```json
{"productId":"10003","inStock":false,"availableQuantity":0,"lastUpdated":"2026-03-18T18:20:00Z"}
```

**404 Not Found** (well-formed productId, no such SKU)

```bash
curl http://localhost:8080/availability/55555555
```
```json
{"status":404,"error":"Not Found","message":"Product not found: 55555555","timestamp":"..."}
```

**400 Bad Request** (malformed productId — non-numeric, empty, null, or contains whitespace)

```bash
curl http://localhost:8080/availability/abc123
curl http://localhost:8080/availability/
curl "http://localhost:8080/availability/123%20456"
```
```json
{"status":400,"error":"Bad Request","message":"productId must be 1-20 numeric digits with no spaces; got: 'abc123'","timestamp":"..."}
```

**429 Too Many Requests** (bonus: rate limit exceeded — 20 requests/sec/IP, token bucket)

```json
{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded, retry shortly","timestamp":"..."}
```

**500 Internal Server Error** — reserved for unexpected data-source failures, never for a malformed or missing productId.

Seeded demo SKUs: `10001`-`10010` (see `InMemoryProductRepository`). `10003` and `10006` are seeded with quantity 0 to demonstrate `inStock: false`.

### Interactive docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

### Health check

- `http://localhost:8080/actuator/health`

## Design notes

- **Validation boundary**: the `productId` format is validated once, in `ProductIdValidator`, called at the top of the service layer before any data access — a malformed ID never reaches the repository.
- **400 vs 404 vs 500**: 400 = can't even parse the input; 404 = well-formed ID, no such product; 500 = reserved for unexpected data-source failure, never used for a bad or missing ID.
- **Caching**: in-process (Caffeine), 8s TTL, keyed by productId. This is a single-instance demo — see the presentation for how a multi-instance deployment needs a shared cache (Memorystore) instead, since each instance otherwise holds its own independent, inconsistent copy.
- **Rate limiting**: token bucket per client IP (20 req/sec, via `X-Forwarded-For` when present), implemented as a servlet filter ahead of the controller. This is application-level, fine-grained protection; edge-level protection (Cloud Armor / load balancer) is a separate, coarser layer meant to sit in front of this in production.
- **Concurrency**: this is a read-only endpoint; the in-memory repository uses a `ConcurrentHashMap`, so concurrent reads are safe. Write-side concurrency (e.g. decrementing stock safely under concurrent cart adds) is out of scope for this exercise and is discussed in the presentation instead.

## Deployment (Google Cloud Run)

Deployed with a single command — Cloud Build compiles the Dockerfile and pushes the image, no local Docker required:

```bash
gcloud run deploy product-availability-service \
  --source . \
  --region us-west1 \
  --platform managed \
  --allow-unauthenticated \
  --min-instances 1 \
  --max-instances 1 \
  --project <your-project-id>
```

Live demo instance: `https://product-availability-service-829547918263.us-west1.run.app`

```bash
curl https://product-availability-service-829547918263.us-west1.run.app/availability/10001
```

Notes:
- `--source .` uploads this directory to Cloud Build, which builds the `Dockerfile` and deploys the resulting image — the same Dockerfile used for local Docker runs above.
- `--min-instances 1 --max-instances 1` pins this to a single instance for the demo (see the presentation for why, and how the production target scales this out with a shared cache).
- CORS is enabled on `/availability/**` (see `WebConfig`) so the endpoint can be called directly from a browser-based demo page, not just curl/Postman.
- On a fresh GCP project, the default Compute Engine service account may need `roles/storage.objectViewer` and `roles/artifactregistry.writer` granted before the first deploy succeeds (Cloud Build uses this account to read the uploaded source and push the built image).
- `server.forward-headers-strategy: framework` (see `application.yml`) is required behind Cloud Run's TLS-terminating proxy — without it, springdoc generates `http://` server URLs even though the service is only reachable over `https://`, which breaks Swagger UI's "Try it out" with a mixed-content error in the browser.

### Redeploying after a code change

**Manual:** re-run the same `gcloud run deploy` command above from the project root. Cloud Build rebuilds the image from the current source and Cloud Run rolls out a new revision, shifting 100% of traffic to it; the previous revision stays available for rollback (`gcloud run services update-traffic`) but serves no traffic.

**Automatic (GitHub → Cloud Run):** this repo is connected to Cloud Run via a Cloud Build trigger — every push to `main` automatically rebuilds the Dockerfile and deploys a new revision of `product-availability-service` in `us-west1`, with no manual step. Set up once via Cloud Run's console → **Continuously deploy new revisions** → GitHub → authorize the Google Cloud Build GitHub App → select this repo; that flow creates the trigger directly (no separate `cloudbuild.yaml` needed, since it builds from the Dockerfile inline). Build progress and history: `https://console.cloud.google.com/cloud-build/builds`.

## Bonus items included

- [x] Unit tests (validator, service, controller — 26 tests)
- [x] Dockerfile (multi-stage build, JRE-only runtime image)
- [x] Basic rate limiting (token bucket per IP)
- [x] API caching (in-process, TTL-based)
- [x] Swagger/OpenAPI spec (springdoc, auto-generated from annotated controller)
