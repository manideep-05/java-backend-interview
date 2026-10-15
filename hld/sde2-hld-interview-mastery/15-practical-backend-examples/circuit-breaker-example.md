# Circuit Breaker (Java / Spring Boot + Resilience4j)

> Concept: [circuit-breakers-and-bulkheads](../06-distributed-systems/circuit-breakers-and-bulkheads.md) · [circuit-breaker-pattern](../07-reliability-and-resilience/circuit-breaker-pattern.md) · [graceful-degradation](../06-distributed-systems/graceful-degradation.md)

**HLD point:** when a dependency is sustained-down, stop calling it (trip **open**), **fail fast**, serve a fallback, and periodically probe for recovery — preventing cascading failure. Trip on **slow calls too**, not only errors.

**Where the concern lives:** app (Resilience4j decorators); the fallback is graceful degradation.

```yaml
# application.yml — Resilience4j circuit breaker
resilience4j:
  circuitbreaker:
    instances:
      notificationSvc:
        failure-rate-threshold: 50          # trip if >50% of calls fail (rate over a window)
        slow-call-rate-threshold: 50        # ALSO trip on slow calls (a slow dep is as dangerous as a dead one)
        slow-call-duration-threshold: 200ms
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        wait-duration-in-open-state: 30s    # cooldown before probing
        permitted-number-of-calls-in-half-open-state: 3
  bulkhead:
    instances:
      notificationSvc:
        max-concurrent-calls: 10            # isolate resources so this dep can't exhaust all threads
```

```java
@Service
class NotificationClient {

    // Breaker + bulkhead + a fallback (graceful degradation when open/failing).
    @CircuitBreaker(name = "notificationSvc", fallbackMethod = "queueForLater")
    @Bulkhead(name = "notificationSvc")
    public void notify(NotificationEvent evt) {
        httpClient.post("/notify", evt);    // the (potentially flaky) dependency call
    }

    // Fallback: don't fail the user-facing flow — degrade (queue it, retry later).
    private void queueForLater(NotificationEvent evt, Throwable t) {
        outbox.enqueue(evt);                // delivered when the dependency recovers
        metrics.increment("notify.degraded");
    }
}
```

**Circuit breaker states (say these):** **Closed** (normal, counting failures) → **Open** (trip: fail fast *without* calling the dependency, for a cooldown) → **Half-open** (allow a few probes; success → Closed, failure → Open).

**Key points to say:**
- When a dependency is down, continuing to call it wastes threads (each call hangs to its timeout) and worsens its overload. The breaker **fails fast** → frees resources, lets the dependency recover.
- **Trip on slow calls, not just errors** — a slow dependency exhausts resources (Little's Law) even without failing. Many candidates miss this.
- **Bulkhead** isolates a per-dependency thread pool so one failing dependency can't consume *all* threads (protects unrelated calls).
- Always pair with a **fallback** (serve stale/cached, queue-for-later, default) — graceful degradation (see [graceful-degradation](../06-distributed-systems/graceful-degradation.md)). An open breaker with no fallback is just a fast error.
- Combine with **timeouts** (bound each call) and **backoff retries** (transient blips) — the resilience trio.
- **Export the breaker state** as a metric + alert (an open breaker is an incident signal).

**Caveat:** production needs tuned thresholds (avoid false trips), observability on breaker state, and a real fallback (not a swallowed error).

**Maps to your resume:** your EPDDMS↔VDMS fault-tolerant integration — a circuit breaker + fallback is how you keep the system serving when the partner system is down, instead of cascading.
