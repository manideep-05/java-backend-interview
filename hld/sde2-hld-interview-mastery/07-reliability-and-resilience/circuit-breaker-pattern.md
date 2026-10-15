# Circuit Breaker Pattern (Reliability View)

> Prerequisites: [06-distributed-systems/circuit-breakers-and-bulkheads](../06-distributed-systems/circuit-breakers-and-bulkheads.md)
> Related: [timeouts-retries-and-deadlines](./timeouts-retries-and-deadlines.md) · [graceful-shutdown-and-draining](./graceful-shutdown-and-draining.md)

The circuit breaker is covered in depth in the distributed-systems section. This page is the reliability-engineering view: tuning, observability, and operating breakers in production.

> Full mechanism (states, bulkheads, resilience trio): [circuit-breakers-and-bulkheads](../06-distributed-systems/circuit-breakers-and-bulkheads.md). This page covers tuning and operation.

---

## A. Problem and motivation

A circuit breaker prevents cascading failure by failing fast when a dependency is sustained-down. But a *badly tuned* breaker causes its own problems: too sensitive → it trips on transient blips (unnecessary degradation); too lax → it doesn't protect in time (cascade still happens). Operating breakers well — tuning thresholds, observing their state, and defining fallbacks — is a reliability skill distinct from knowing the pattern.

---

## B. Tuning parameters

| Parameter | What it controls | Tuning guidance |
|---|---|---|
| **Failure threshold** | How many failures (% or count) trip it open | Too low → false trips on blips; too high → slow to protect. Often a failure *rate* over a rolling window (e.g., >50% of last 20 calls). |
| **Rolling window** | The period/count failures are measured over | Big enough to be statistically meaningful, small enough to react |
| **Open duration (cooldown)** | How long it stays open before probing | Long enough to give the dependency recovery time; short enough to recover service quickly |
| **Half-open probe count** | How many test requests when probing | A few; too many on a still-sick dependency re-overloads it |
| **Slow-call threshold** | Treat slow (not just failed) calls as failures | Trip on latency, not only errors — a slow dependency is as dangerous as a dead one |

**Key insight:** trip on *slow* calls too, not just errors. A dependency that's responding but very slowly still exhausts your resources (Little's Law) and should trip the breaker.

---

## C. Observability (operating it)

A breaker is only useful if you can see it:
- **Emit the breaker state** (closed/open/half-open) as a metric and alert on it — an open breaker is an incident signal.
- **Track trip frequency** — frequent trips mean a flaky dependency or mis-tuned thresholds.
- **Correlate** breaker opens with the dependency's own metrics to find root cause.

Without observability, breakers silently mask problems — the dependency is down, the breaker is open, users get fallbacks, and nobody knows until the fallback itself becomes inadequate. See [logs-metrics-and-traces](../09-observability-and-operations/logs-metrics-and-traces.md).

---

## D. Example — DMS

The DMS notification-service breaker: trips open if >50% of calls in a rolling 10s window fail *or* exceed the slow-call threshold (200ms); stays open 30s; then half-open with 3 probes. State is exported as a metric; an alert fires when it opens (so on-call knows the notification service is down, even though users aren't impacted — notifications queue for later). This gives both protection (uploads unaffected) and visibility (the team knows and fixes the root cause).

---

## E. Trade-offs / failure scenarios

| Mis-tuning | Effect | Fix |
|---|---|---|
| Threshold too low | False trips on transient blips → needless degradation | Raise threshold; use a rate over a window |
| Threshold too high | Slow to trip → cascade before protection | Lower threshold; add slow-call tripping |
| Cooldown too short | Flaps open/closed, re-hammering a sick dependency | Lengthen cooldown; limit half-open probes |
| No observability | Breaker masks the problem silently | Export state as metric + alert |
| No fallback when open | Hard errors instead of degradation | Define a fallback (see [graceful-degradation](../06-distributed-systems/graceful-degradation.md)) |

---

## F. Scaling implications

In a large service mesh, breakers are standard on every inter-service call (often provided by the mesh/library), and their tuning + observability become operational concerns at fleet scale. Tripping on slow calls (not just errors) matters more as traffic grows (slow dependencies cause the biggest cascades). Breaker-state dashboards become a key signal for diagnosing "which dependency is unhealthy" during incidents. The breaker is the reliability backbone that converts dependency failures into contained degradations.

---

## G. Interview preparation

**Basic**
- Q: What does a circuit breaker protect against? → *A: Cascading failure — it fails fast when a dependency is sustained-down, freeing resources and letting the dependency recover.*

**Intermediate**
- Q: How do you tune a circuit breaker? → *A: Failure *rate* threshold over a rolling window (not too sensitive/lax), a cooldown that gives recovery time, limited half-open probes, and crucially trip on *slow* calls too — a slow dependency is as dangerous as a dead one.*
- Q: Why export breaker state? → *A: An open breaker is an incident signal; without observability, breakers silently mask a down dependency until the fallback becomes inadequate.*

**SDE-2 / scenario**
- Q: "Your breaker trips during every minor blip." → *A: Threshold too sensitive — switch to a failure *rate* over a rolling window, raise the threshold, and ensure the cooldown isn't causing flapping. Validate against the dependency's normal error/latency profile.*
- Q: "A dependency is slow but not erroring, and it's taking us down." → *A: Add slow-call tripping so the breaker opens on latency, not just errors — a slow dependency exhausts resources (Little's Law) even without failing.*

**Common wrong answers:** tripping only on errors (not slow calls); no observability; over-sensitive thresholds; no fallback.

---

## H. Hands-on exercise

Tune a circuit breaker for a payment dependency with p99=300ms and occasional transient blips. Specify: failure-rate threshold + window, slow-call threshold, cooldown, half-open probes, the metric/alert you'd emit, and the fallback when open.

---

## I. Revision summary

**Key takeaways**
- Tune breakers: failure-rate over a rolling window, sensible cooldown, limited half-open probes; **trip on slow calls too**.
- Make breakers observable: export state, alert on open, correlate with dependency metrics.
- Always pair with a fallback (graceful degradation) for the open state.
- Mis-tuning causes false trips or late protection; observability prevents silent masking.

**Rapid-fire**
1. Trip only on errors? → No — also on slow calls.
2. Threshold form that avoids false trips? → Failure *rate* over a rolling window.
3. Why export breaker state? → Open = incident signal; avoid silent masking.
4. Too-short cooldown causes? → Flapping (re-hammering a sick dependency).
5. Open state needs a? → Fallback.

**Next:** [graceful-shutdown-and-draining](./graceful-shutdown-and-draining.md)
