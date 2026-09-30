# Product Availability Service

A small REST microservice that exposes real-time product availability for a single SKU, built for Aritzia's (Senior) Developer, Backend case assessment.

## Tech stack

- Java 21
- Spring Boot 3.3 (Web, Cache, Actuator)
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

Request flow: `RequestLoggingFilter` (assigns a request ID, logs status and latency) -> `RateLimitFilter` (servlet filters, run before Spring MVC) -> `ProductAvailabilityController` -> `ProductAvailabilityService` (validates, then checks cache, then repository on a miss) -> `InMemoryProductRepository`.

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

Other routes and methods keep their proper HTTP meaning rather than falling into the error handler above:
- Unknown routes (e.g. `/foo`, `/availability/1/2`) → **404**, `"No route for /foo"`
- Wrong method (e.g. `POST /availability/10001`) → **405** with an `Allow: GET` header

Every response carries an `X-Request-Id` header. Send your own (1–64 characters of letters, digits, `.`, `_`, `-`) to trace a call across services; otherwise one is generated.

Seeded demo SKUs: `10001`-`10010` (see `InMemoryProductRepository`). `10003` and `10006` are seeded with quantity 0 to demonstrate `inStock: false`.

### Interactive docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

### Health check

- `http://localhost:8080/actuator/health`

## Design notes

- **Validation boundary**: the `productId` format is validated once, in `ProductIdValidator`, called at the top of the service layer before any data access — a malformed ID never reaches the repository.
- **400 vs 404 vs 500**: 400 = can't even parse the input; 404 = well-formed ID, no such product; 500 = reserved for unexpected data-source failure, never used for a bad or missing ID.
- **Negative stock**: the spec only defines `quantity > 0` (in stock) and `quantity = 0` (out of stock). A negative quantity is treated as corrupt data: `Product` refuses to be constructed with one, so it surfaces as a 500 and an ERROR log instead of being served as `availableQuantity: -3`.
- **Caching**: in-process (Caffeine), 8s TTL, keyed by the *validated* productId (so `" 10001"` and `"10001"` share one entry). Storefront and product pages can tolerate a few seconds of staleness; checkout should not rely on this read and would reserve stock at the order management system instead. This is a single-instance demo — see the presentation for how a multi-instance deployment needs a shared cache (Memorystore), since each instance otherwise holds its own independent, inconsistent copy.
- **Rate limiting**: token bucket per client IP (20 req/sec, via `X-Forwarded-For` when present), implemented as a servlet filter ahead of the controller; a 429 uses the same error body as every other error. Known limitation: the IP comes from a header the caller can set, so this is a demo-grade guard. Since the callers are internal services, production limits belong at the gateway (Apigee / API Gateway), keyed by API key rather than IP.
- **Concurrency**: this is a read-only endpoint; the in-memory repository uses a `ConcurrentHashMap`, so concurrent reads are safe. Write-side concurrency (e.g. decrementing stock safely under concurrent cart adds) is out of scope for this exercise and is discussed in the presentation instead.
- **Untrusted input in logs and errors**: any caller-supplied value echoed into a log line or error message (productId, request path) is truncated to 50 characters and has control characters replaced (`SafeText`), so oversized input can't bloat logs and embedded newlines can't forge fake log entries.
- **Logging**: `RequestLoggingFilter` runs first on every request, assigns a request ID (MDC + `X-Request-Id` response header), and logs one summary line per request — method, path, status, latency. Locally, logs are plain text with the request ID in brackets. In the container (`SPRING_PROFILES_ACTIVE=cloud`, set in the Dockerfile) they're one JSON object per line with `severity`, `message`, `time` and `requestId`, which Cloud Logging parses natively — so severity filters work and all lines for one request can be pulled up by its ID.
- **Container**: the runtime image runs as an unprivileged `app` user, not root.

### CORS

`WebConfig` allows cross-origin `GET` requests to `/availability/**` from any origin (`Access-Control-Allow-Origin: *`).

**Why it's needed:** browsers block a page from reading responses from a different origin unless the server opts in. The live demo console (`dashboard.html`) is served from a different origin than the Cloud Run service, so without this header its `fetch()` calls would reach the service but the browser would refuse to hand the response to the page. curl, Postman and server-to-server callers don't enforce CORS at all — it only matters for browsers.

**Why allow-all is acceptable here:** the endpoint is read-only (`GET` only), unauthenticated, uses no cookies or credentials, and returns non-sensitive demo data. CORS isn't access control; it only controls which *web pages* may read responses in a browser, and anyone can already call this endpoint directly. The scope is also narrow: only `/availability/**` and only `GET`, not actuator or other routes.

**Production:** the real callers (checkout, storefront) are internal services, so the service would sit behind Cloud Run IAM or service-to-service authentication via a gateway rather than being publicly reachable. If a browser client did need direct access, the origin list would be restricted to the specific storefront domains rather than `*`.

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
- CORS is enabled on `/availability/**` so the endpoint can be called directly from a browser-based demo page — see [CORS](#cors) above for why and what changes in production.
- On a fresh GCP project, the default Compute Engine service account may need `roles/storage.objectViewer` and `roles/artifactregistry.writer` granted before the first deploy succeeds (Cloud Build uses this account to read the uploaded source and push the built image).
- `server.forward-headers-strategy: framework` (see `application.yml`) is required behind Cloud Run's TLS-terminating proxy — without it, springdoc generates `http://` server URLs even though the service is only reachable over `https://`, which breaks Swagger UI's "Try it out" with a mixed-content error in the browser.

### Redeploying after a code change

**Manual:** re-run the same `gcloud run deploy` command above from the project root. Cloud Build rebuilds the image from the current source and Cloud Run rolls out a new revision, shifting 100% of traffic to it; the previous revision stays available for rollback (`gcloud run services update-traffic`) but serves no traffic.

**Automatic (GitHub → Cloud Run):** this repo is connected to Cloud Run via a Cloud Build trigger — every push to `main` automatically rebuilds the Dockerfile and deploys a new revision of `product-availability-service` in `us-west1`, with no manual step. Set up once via Cloud Run's console → **Continuously deploy new revisions** → GitHub → authorize the Google Cloud Build GitHub App → select this repo; that flow creates the trigger directly (no separate `cloudbuild.yaml` needed, since it builds from the Dockerfile inline). Build progress and history: `https://console.cloud.google.com/cloud-build/builds`.

## Presentation and live demo

`docs/` holds the case-study write-up and demo console. Open `docs/report.html` directly in a browser, with the other files in the same folder:

- `report.html` — the full write-up: problem, architecture, design decisions, quality, path to production, and an appendix. Its "Live demo" section embeds the console below.
- `dashboard.html` — a browser console that calls the deployed service with real `fetch()` requests, shows response history, and demonstrates caching (repeat requests within the 8s TTL return faster). It's pre-filled with the live Cloud Run URL.
- `demo-architecture.svg`, `prod-architecture.svg` — the architecture diagrams.

## Bonus items included

- [x] Unit and integration tests — 47 tests covering validation edge cases, every status code (200/400/404/405/429/500), caching, rate limiting, request IDs and log formatting
- [x] Dockerfile (multi-stage build, JRE-only runtime image, runs as a non-root user)
- [x] Basic rate limiting (token bucket per IP)
- [x] API caching (in-process, TTL-based)
- [x] Swagger/OpenAPI spec (springdoc, auto-generated from annotated controller)
