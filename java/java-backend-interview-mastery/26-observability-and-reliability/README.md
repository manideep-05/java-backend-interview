# 26 — Observability & Reliability

> **Priority: P1.** You can't operate what you can't see. The signal is knowing when to reach for metrics vs logs vs traces, and the reliability patterns (timeouts, retries, circuit breakers, SLOs) that keep a service up under partial failure.

**Prerequisites:** [22 Debugging](../22-debugging-and-production-engineering/), [18 API](../18-api-engineering/).

---

## A. Concept & intuition
Observability is the ability to answer "what is my system doing and why" from its outputs. Reliability is designing so partial failures don't become total outages. They reinforce each other: good signals let you detect and diagnose; good reliability patterns limit blast radius while you do.

## B. Core concepts

### The three pillars (metrics, logs, traces)
- **Metrics:** aggregated numeric time series (request rate, error rate, latency percentiles, pool usage). Cheap, great for dashboards/alerts and spotting *that* something is wrong. Use **percentiles (p95/p99)**, not averages (averages hide tail latency).
- **Logs:** discrete, detailed events — great for the *why* of a specific request. Use **structured** logs (JSON, key-value) with a **correlation/trace id** so you can stitch a request across services.
- **Traces:** the path of one request across services with per-span timing — show *where* latency/errors occur in a distributed call graph. OpenTelemetry is the standard.

Rule of thumb: metrics tell you something's wrong and alert you; traces localize it to a service/span; logs explain the specific failure.

### The four golden signals
Latency, traffic, errors, saturation — a compact dashboard for any service.

### SLI / SLO / error budget
- **SLI:** a measured indicator (e.g., % of requests < 300 ms).
- **SLO:** the target (e.g., 99.9% of requests succeed).
- **Error budget:** the allowed failure (1 − SLO) that governs how aggressively you ship vs stabilize. SLOs focus alerting on user impact, not noise.

### Health checks
Liveness (is the process alive — restart if not) vs readiness (can it serve traffic — remove from the LB if not). Spring Boot Actuator `/health` with groups ([10](../10-spring-boot-internals/)). Don't let a readiness check depend on a flaky downstream in a way that flaps.

### Reliability patterns
- **Timeouts** on every call (no infinite waits).
- **Retries** with backoff + jitter + budget (only idempotent ops) — [18 API-06](../18-api-engineering/).
- **Circuit breaker:** after N failures, "open" and fail fast for a cooldown, then "half-open" to probe recovery — stops hammering a down dependency and lets it recover.
- **Bulkhead:** isolate resources (separate thread pools/connection limits per dependency) so one slow dependency can't exhaust everything.
- **Graceful degradation / fallback:** serve cached/default/partial results when a dependency is down.
- **Graceful shutdown:** stop accepting new work, finish in-flight requests, release resources ([24](../24-build-tools-and-deployment-fundamentals/)).

### Alerting
Alert on **symptoms that affect users** (SLO burn, error rate, latency) rather than every metric; avoid alert fatigue; make alerts actionable with runbooks.

## C. Code
```java
// Micrometer metrics (Actuator exposes them) + correlation id in logs
@Timed(value = "orders.create", percentiles = {0.95, 0.99})   // latency percentiles, not average
public OrderResponse create(CreateOrder req) { ... }

// Correlation id via MDC filter so every log line + downstream call carries it
MDC.put("traceId", traceId);   // structured logs include traceId; remove in finally to avoid leak
```
```yaml
# Resilience4j: timeout + retry(backoff+jitter) + circuit breaker
resilience4j.circuitbreaker.instances.payments:
  failureRateThreshold: 50
  waitDurationInOpenState: 10s
  slidingWindowSize: 20
resilience4j.retry.instances.payments:
  maxAttempts: 3
  waitDuration: 200ms
  enableExponentialBackoff: true
  enableRandomizedWait: true     # jitter
```

## D. Trade-offs

| Signal | Strength | Weakness | Reach for it to |
|---|---|---|---|
| Metrics | cheap, aggregate, alertable | no per-request detail | detect + alert, dashboards |
| Logs | detailed, per-event | volume/cost, hard to aggregate | explain a specific failure |
| Traces | per-request path | sampling, setup cost | localize latency/errors across services |

| Pattern | Protects against | Cost |
|---|---|---|
| Timeout | hung dependencies | may cut slow-but-valid calls |
| Circuit breaker | hammering a down dep | tuning thresholds |
| Bulkhead | cross-dependency exhaustion | more pools to size |
| Retry+jitter | transient blips | storms if misused |

## E. Interview Q&A

### OBS-01 (P1) — Metrics vs logs vs traces — when do you use each?
**Strong answer:** Metrics are aggregated time series — cheap, perfect for dashboards and alerts, and they tell me *that* something's wrong (error rate up, p99 latency up); I use percentiles, not averages, because averages hide the tail. Traces follow a single request across services with per-span timing and localize *where* the latency or error is. Logs give the detailed *why* of a specific request, especially structured logs with a correlation id. In an incident I go metrics → trace → logs: detect, localize, explain.

<a id="obs-02"></a>
### OBS-02 (P1) — Why percentiles instead of average latency?
**Strong answer:** Averages are dominated by the common fast case and hide the slow tail that users actually feel — a service can have a great average while 1% of requests (p99) time out. I track p95/p99 (and max) so tail latency and SLO violations are visible, and I alert on percentile breaches, not the mean.

### OBS-03 (P1) — What are SLIs, SLOs, and error budgets?
**Strong answer:** An SLI is a measured indicator of user-visible health (e.g., fraction of requests under 300 ms or succeeding); an SLO is the target for it (99.9%); the error budget is 1 − SLO, the allowed failure that governs the balance between shipping features and stabilizing. SLOs make alerting about user impact rather than raw metric noise, and the budget gives an objective signal to slow down when reliability is at risk.

### OBS-04 (P1) — How does a circuit breaker work and why use it?
**Strong answer:** It tracks failure rate; once failures cross a threshold it "opens" and fails fast (or falls back) for a cooldown instead of calling the failing dependency, then goes "half-open" to probe with a few requests and closes if they succeed. It stops a struggling dependency from being hammered (which worsens outages and ties up my threads) and lets it recover, while giving callers a fast failure/fallback instead of a hang. I pair it with timeouts and bulkheads.

### OBS-05 (P1) — Liveness vs readiness probes?
**Strong answer:** Liveness answers "is the process healthy enough to keep running" — failing it restarts the pod. Readiness answers "can it serve traffic right now" — failing it removes the instance from the load balancer without killing it (e.g., during startup or when a critical dependency is down). I keep readiness from flapping on transient downstream blips and use Actuator health groups to separate them.

### OBS-06 (P2) — What is a bulkhead and when do you need it?
**Strong answer:** Isolating resources per dependency — e.g., separate thread pools or connection limits — so one slow/failing dependency can only exhaust its own partition, not the whole service's threads. I use it when a service calls multiple downstreams of differing reliability, so a problem in one is contained.

## F. Production relevance
Observability and reliability are what make a service operable: percentile metrics and SLO-based alerts catch user-impacting problems early; traces + correlation ids turn a cross-service mystery into a located span; circuit breakers, timeouts, bulkheads, and graceful degradation stop one failing dependency from cascading into an outage (the retry-storm/thread-exhaustion scenarios in [22](../22-debugging-and-production-engineering/)); and liveness/readiness probes plus graceful shutdown make deploys and autoscaling safe. The mature answer ties a symptom to the signal that reveals it and the pattern that contains it.

## G. Revision

### 10 key takeaways
1. Metrics (detect/alert) → traces (localize) → logs (explain).
2. Use p95/p99, not averages.
3. Four golden signals: latency, traffic, errors, saturation.
4. SLI/SLO/error budget focus alerting on user impact.
5. Structured logs + correlation id stitch requests across services.
6. Timeouts on every call.
7. Circuit breaker: open → half-open → closed; fail fast, recover.
8. Bulkhead isolates per-dependency resources.
9. Liveness (restart) vs readiness (remove from LB).
10. Graceful degradation + shutdown keep deploys/outages safe.

### 10 rapid-fire
1. Detect "something's wrong"? (metrics) 2. Localize across services? (traces) 3. Explain one request? (logs) 4. Latency metric to track? (p99) 5. 1−SLO? (error budget) 6. Fail fast on a down dep? (circuit breaker) 7. Isolate pools per dep? (bulkhead) 8. Remove from LB? (readiness) 9. Restart pod? (liveness) 10. Stitch logs by? (correlation id)

### 5 tricky
1. Why can a healthy average hide an outage? (tail latency) 2. Why does a circuit breaker help a struggling dependency? 3. Readiness vs liveness misuse consequence? (flapping/killing) 4. Why alert on SLO burn, not every metric? 5. Why add bulkheads with multiple downstreams?

### 5 exercises
1. Add Micrometer `@Timed` with p95/p99 and view it in Actuator/Prometheus.
2. Propagate a correlation id via MDC through logs and a downstream call.
3. Configure Resilience4j circuit breaker + timeout + retry(jitter) and trip it.
4. Define an SLO and an alert on its error budget burn.
5. Separate liveness/readiness with Actuator health groups and test behavior when a dependency is down.

### One-page revision sheet
> **Metrics(detect/alert) → traces(localize) → logs(explain).** p95/p99 not averages. Golden signals: latency/traffic/errors/saturation. SLI/SLO/**error budget**(1−SLO) → alert on user impact. Structured logs + **correlation id**. **Timeouts everywhere.** **Circuit breaker**: open→half-open→closed (fail fast, recover). **Bulkhead** isolates per-dep pools. **Liveness**(restart) vs **readiness**(remove from LB). Graceful degradation + shutdown.

### Prerequisites to revisit
- Debugging playbooks (ch.22); retries/timeouts (ch.18); actuator (ch.10).
