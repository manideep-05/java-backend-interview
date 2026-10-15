# 18 — API Engineering & Backend Fundamentals

> **Priority: P1 (with P0 idempotency/retry topics).** The signal is designing APIs that stay correct under network failure, client retries, partial success, and concurrency — not just REST verbs.

**Prerequisites:** [11 Spring MVC](../11-spring-mvc-and-rest-apis/), [23 HTTP](../23-networking-and-http-fundamentals/).
**Related:** [17 DB idempotency](../17-database-transactions-and-isolation/), [19 Messaging](../19-messaging-and-asynchronous-processing/), [26 Observability](../26-observability-and-reliability/).

---

## A. Concept & intuition
An API is a contract: clients depend on its shape and behavior. Good API engineering makes that contract clear, versioned, and resilient to the realities of distributed systems — networks drop, clients retry, requests arrive concurrently, and dependencies fail. The hardest part isn't the happy path; it's defining behavior under failure.

## B. Core concepts

### HTTP methods: safe vs idempotent
- **Safe:** no server state change (`GET`, `HEAD`, `OPTIONS`).
- **Idempotent:** same effect whether called once or N times (`GET`, `PUT`, `DELETE` — by HTTP semantics). `POST` is **not** idempotent by default.
- **Idempotent method ≠ idempotency key** (API-03): HTTP *method* idempotency is a semantic property; an **idempotency key** is a concrete dedup mechanism (a client-supplied unique id you store) to make a non-idempotent `POST` safe to retry.

### Status codes & error design
200/201/204 success; 400 validation; 401/403 authn/authz; 404 not found; 409 conflict (optimistic lock / unique violation); 422 unprocessable; 429 rate limited; 5xx server. Return a consistent error body (`ProblemDetail`/RFC 7807) with a stable code and a correlation id; never leak stack traces.

### Retries, timeouts, backoff
Every network call needs a **timeout** (never infinite). Retries must use **exponential backoff with jitter** to avoid synchronized retry storms ([DBG-11](../22-debugging-and-production-engineering/)), a **retry budget**/cap, and should only retry **idempotent** or idempotency-keyed operations. A **circuit breaker** stops hammering a failing dependency.

### Idempotency keys & duplicate requests
Client sends `Idempotency-Key: <uuid>` on a `POST`; the server records it with the result; a retry with the same key returns the stored result instead of acting twice. Back it with a unique constraint so concurrent duplicates can't both proceed ([17](../17-database-transactions-and-isolation/)).

### Pagination, filtering, sorting, versioning
Keyset pagination for deep pages ([16](../16-database-indexing-and-query-optimization/)); explicit filter/sort params; version via URL (`/v1`) or header; evolve compatibly (add optional fields, don't remove/rename) and deprecate with notice.

### Bulk APIs & partial failures
A batch can partially succeed; decide and document the contract: all-or-nothing (transaction) vs per-item results (`207 Multi-Status`-style body with per-item status). Return which items failed and why.

### Caching headers & conditional requests
`ETag`/`If-None-Match` and `Last-Modified`/`If-Modified-Since` enable 304 responses; `Cache-Control` governs client/proxy caching ([20](../20-caching-and-performance/)).

### Rate limiting
Protect the service with token-bucket/sliding-window limits; return 429 + `Retry-After`.

## C. Code — an idempotent POST
```java
@PostMapping("/payments")
ResponseEntity<PaymentResponse> create(
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody CreatePayment req) {
    // Insert the key first; a unique constraint makes concurrent duplicates fail cleanly.
    Optional<PaymentResponse> existing = idempotencyStore.find(key);
    if (existing.isPresent()) return ResponseEntity.ok(existing.get()); // retry -> stored result
    PaymentResponse result = service.create(req);
    idempotencyStore.save(key, result); // unique(key) -> second concurrent insert throws -> treat as dup
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
}
```
```java
// Resilient outbound call: timeout + backoff with jitter + circuit breaker (Resilience4j style)
@Retry(name = "svc")            // exponential backoff + jitter configured in properties
@CircuitBreaker(name = "svc", fallbackMethod = "fallback")
PaymentStatus charge(ChargeRequest r) { return client.charge(r); } // client has a connect+read timeout
```

## D. Trade-offs

| Decision | Option A | Option B | Guidance |
|---|---|---|---|
| Dedup | rely on idempotent method | idempotency key + unique constraint | key for non-idempotent POSTs |
| Versioning | URL `/v1` | header | URL is explicit/cache-friendly |
| Batch failure | all-or-nothing | per-item status | per-item for large batches |
| Retry | fixed delay | exponential + jitter | always jitter; add budget/breaker |
| Pagination | OFFSET | keyset | keyset for deep/stable |

## E. Interview Q&A

### API-01 (P1) — Safe vs idempotent methods?
**Strong answer:** Safe methods don't change server state (GET/HEAD); idempotent methods produce the same result whether called once or many times (GET/PUT/DELETE). POST is neither by default. This matters because only idempotent operations are safe to retry blindly; a POST needs an idempotency key to be retry-safe.

### API-02 (P1) — How do you design error responses?
**Strong answer:** Map conditions to the right status (400/401/403/404/409/422/429/5xx), return a consistent body (`ProblemDetail`) with a stable machine-readable code, a safe human message, and a correlation id, and never leak internals/stack traces. The error contract is part of the API and should be documented like the success path.

<a id="api-03"></a>
### API-03 (P0) — Why isn't an idempotent HTTP method the same as an idempotency key?
**Strong answer:** Method idempotency is a *semantic* property of PUT/DELETE/GET — repeating them has the same effect by definition. An idempotency key is a *mechanism*: a client-supplied unique id the server stores so a retried **POST** (which is not idempotent) returns the original result instead of creating a second resource. You need the key precisely because POST lacks semantic idempotency; and you back the key with a unique constraint so concurrent duplicates can't both succeed. They operate at different levels — semantics vs implementation.
**Weak answer:** "They're the same, both prevent duplicates." Misses that POST needs an explicit mechanism.

### API-04 (P1) — How do you handle partial failure in a bulk API?
**Strong answer:** Decide the contract up front: either all-or-nothing in a single transaction (simpler for clients, but one bad item fails the batch) or per-item results where I return each item's status and error so the client can retry only the failures. For large imports I prefer per-item results with a clear summary, plus idempotency so retried items don't duplicate.

### API-05 (P1) — How do you version an API without breaking clients?
**Strong answer:** Evolve compatibly — add optional fields, never remove/rename or change types; introduce a new version (URL `/v2` or header) only for breaking changes; deprecate with a timeline and telemetry on old-version usage. Tolerant readers on both sides reduce coupling.

<a id="api-06"></a>
### API-06 (P0) — Why can adding retries make an outage worse?
**Strong answer:** When a dependency is already struggling, naive retries multiply the load on it, and if every client retries on the same schedule you get a synchronized thundering herd that prevents recovery — the retry traffic becomes the outage. The fixes are exponential backoff **with jitter** to desynchronize, a retry budget/cap so retries can't exceed a fraction of traffic, a circuit breaker to stop calling a failing dependency, and idempotency so the retries that do happen are safe. Retries help transient blips, not a saturated dependency.

### API-07 (P2) — How do conditional requests / ETags work?
**Strong answer:** The server returns an `ETag` (a version/hash) with a resource; the client sends `If-None-Match` on the next request, and the server replies `304 Not Modified` (no body) if unchanged, saving bandwidth. `If-Match` enables optimistic concurrency on writes (reject if the resource changed). It's HTTP-level optimistic locking and caching.

## F. Production relevance
API contracts under failure are where real systems break: non-idempotent POSTs plus client retries create duplicate charges/orders ([17 DBTX-09](../17-database-transactions-and-isolation/)); missing timeouts turn one slow dependency into a cascading hang; naive retries cause retry storms; breaking changes without versioning break clients silently. The durable practices — idempotency keys backed by unique constraints, timeouts everywhere, backoff+jitter+budget+breaker, compatible versioning, per-item batch results, correlation ids — are exactly what interviewers want to hear.

## G. Revision

### 10 key takeaways
1. Safe (no state change) vs idempotent (repeat-safe); POST is neither.
2. Idempotency key = mechanism to make POST retry-safe; back it with a unique constraint.
3. Status codes map to meaning; consistent ProblemDetail errors, no stack traces.
4. Every call has a timeout.
5. Retries: exponential backoff + jitter + budget + breaker, only for idempotent ops.
6. Keyset pagination for deep pages.
7. Version compatibly; add optional, don't remove/rename.
8. Bulk APIs: define all-or-nothing vs per-item.
9. ETags enable 304 and optimistic writes.
10. Rate limit with 429 + Retry-After.

### 10 rapid-fire
1. Is POST idempotent? (no) 2. Make it retry-safe? (idempotency key) 3. Conflict status? (409) 4. Rate-limited status? (429) 5. Retry jitter why? (avoid herds) 6. Deep paging? (keyset) 7. Error format? (ProblemDetail) 8. Breaking change → ? (new version) 9. 304 enabled by? (ETag/If-None-Match) 10. Infinite timeout? (never)

### 5 tricky
1. Why did a retried POST double-charge? 2. Idempotent method vs key difference? 3. Why did retries worsen the outage? 4. Why back an idempotency key with a unique constraint? 5. How to report partial batch failure?

### 5 exercises
1. Add idempotency-key handling to a POST with a unique-constraint-backed store.
2. Configure Resilience4j retry (backoff+jitter) + circuit breaker + timeout on an outbound call.
3. Design a bulk endpoint returning per-item statuses.
4. Add ETag/If-None-Match to a GET and return 304.
5. Add a token-bucket rate limiter returning 429 + Retry-After.

### One-page revision sheet
> Safe(no change) vs idempotent(repeat-safe: GET/PUT/DELETE); **POST neither → idempotency key (mechanism) + unique constraint**. Status: 400/401/403/404/409/422/429/5xx; ProblemDetail + correlation id, no stack traces. **Timeout everywhere.** Retries = exponential **backoff + jitter + budget + circuit breaker**, idempotent only (else storm). Keyset pagination. Version compatibly (add optional). Bulk: all-or-nothing vs per-item 207. ETag/If-None-Match→304 + optimistic writes. Rate limit 429+Retry-After.

### Prerequisites to revisit
- MVC error handling (ch.11); DB idempotency/unique constraints (ch.17); retry storms (ch.22).
