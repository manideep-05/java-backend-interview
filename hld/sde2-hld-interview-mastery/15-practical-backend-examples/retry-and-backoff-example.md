# Retry with Exponential Backoff + Jitter (Java / Spring Boot)

> Concept: [retries-backoff-and-jitter](../05-messaging-and-event-driven-systems/retries-backoff-and-jitter.md) · [retries-and-duplicate-execution](../06-distributed-systems/retries-and-duplicate-execution.md)

**HLD point:** retry *transient* failures with **exponential backoff + jitter** (not immediate/fixed — that causes retry storms and thundering herds), a **bounded budget**, and only for **idempotent/retriable** operations.

**Where the concern lives:** app (Spring Retry / Resilience4j); the retry policy is a deliberate config, not ad-hoc loops.

```java
@EnableRetry
@Configuration
class RetryConfig {}

@Service
class PartnerSyncService {

    // Retry ONLY retriable exceptions; exponential backoff WITH jitter; bounded attempts.
    @Retryable(
        retryFor = { TransientApiException.class, SocketTimeoutException.class }, // 5xx/timeouts
        noRetryFor = { BadRequestException.class },                              // 4xx -> don't retry
        maxAttempts = 5,
        backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 10_000, random = true) // jitter=random
    )
    public void syncDocument(DocRef ref) {
        partnerClient.push(ref); // must be IDEMPOTENT (keyed) so a retry can't duplicate
    }

    // After the budget is exhausted -> this runs (route to DLQ / alert / mark failed).
    @Recover
    public void recover(RuntimeException e, DocRef ref) {
        deadLetter.send(ref, e);   // see dead-letter-handling example
        alerts.raise("sync failed after retries", ref, e);
    }
}
```

Programmatic alternative with Resilience4j (composable with a circuit breaker):
```java
RetryConfig cfg = RetryConfig.custom()
    .maxAttempts(5)
    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
        Duration.ofMillis(500), 2.0, 0.5))  // base 500ms, x2, 50% jitter
    .retryOnException(ex -> ex instanceof TransientApiException)
    .build();
Retry retry = Retry.of("partnerSync", cfg);
Runnable decorated = Retry.decorateRunnable(retry, () -> partnerClient.push(ref));
```

**Key points to say:**
- **Exponential backoff** gives the dependency time to recover (not immediate retries that hammer it); **jitter** (`random = true`) prevents many clients synchronizing into retry spikes (thundering herd).
- **Only retry retriable errors** (5xx/timeouts/connection) — never 4xx (the request is wrong; retrying wastes capacity).
- **Only retry idempotent operations** (or with an idempotency key) — else a retry after a timeout where the original succeeded duplicates the effect (see [retries-and-duplicate-execution](../06-distributed-systems/retries-and-duplicate-execution.md)).
- **Bounded budget** (`maxAttempts`) + a `@Recover` path (→ DLQ/alert) — unbounded retries cause loops and resource exhaustion.
- Pair with a **circuit breaker** (backoff for transient blips; breaker for a dependency that's clearly down — see [circuit-breaker-example](./circuit-breaker-example.md)).

**Caveat:** production needs the recover path wired to a DLQ, metrics on retry/exhaustion rates, and deadline-awareness (retries must fit within the request's overall deadline).

**Maps to your resume:** your notification platform's "5 retries before publishing, then DLQ" and your EPDDMS↔VDMS "retry mechanisms" are exactly this — add the words "exponential backoff + jitter" and "retriable vs non-retriable" when you tell it.
