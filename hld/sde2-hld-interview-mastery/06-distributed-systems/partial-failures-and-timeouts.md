# Partial Failures and Timeouts

> Prerequisites: [why-distributed-systems](./why-distributed-systems.md)
> Related: [circuit-breakers-and-bulkheads](./circuit-breakers-and-bulkheads.md) · [retries-and-duplicate-execution](./retries-and-duplicate-execution.md) · [00-foundations/latency-throughput-and-capacity](../00-foundations/latency-throughput-and-capacity.md)

Partial failure is the defining distributed-systems problem, and timeouts are the primary tool to cope with the "slow vs dead" uncertainty. Getting timeouts wrong is a leading cause of cascading outages.

---

## A. Problem and motivation

When service A calls service B, B might be fine, slow, overloaded, crashed, or unreachable — and A **cannot tell which**. If A waits indefinitely for B, A's threads/connections pile up (Little's Law — see [latency](../00-foundations/latency-throughput-and-capacity.md)), A slows, and A's callers back up too: one slow dependency cascades into a system-wide outage. Timeouts bound the wait so A can give up and recover. But timeouts introduce their own uncertainty (did B actually do the work?) and must be tuned carefully.

---

## B. The slow-vs-dead problem

```mermaid
sequenceDiagram
    participant A as Service A
    participant B as Service B
    A->>B: request
    Note over B: B is slow / overloaded / crashed / unreachable - A can't tell which
    A--xA: waiting... threads pile up
    Note over A: without a timeout, A hangs; with one, A gives up after T
```

A has only one signal: whether a response arrived in time. It cannot distinguish "B crashed" from "B is slow" from "the network dropped it." The timeout is a *decision* ("I'll assume failure after T"), and it can be wrong — B might have succeeded just after A gave up. This ambiguity is why retries need idempotency (next chapter).

---

## C. Why no-timeout (or too-long) is catastrophic

The classic cascade:
1. B gets slow (DB contention, GC, overload).
2. A's calls to B take longer; A's threads/connections stay busy (`concurrency = RPS × latency` rises — Little's Law).
3. A's thread/connection pool exhausts; A can't serve *any* request (even ones not needing B).
4. A's callers now time out on A; the failure propagates upstream.

A single slow leaf takes down the whole tree. Timeouts (and bulkheads/circuit breakers) break this chain by freeing A's resources quickly.

```mermaid
flowchart LR
    B[B slow] --> A1[A threads held longer] --> A2[A pool exhausted] --> A3[A fails all requests] --> U[Callers of A fail -> cascade]
```

---

## D. Setting timeouts well

- **Always set timeouts** on every network call (DB, cache, HTTP, RPC). A missing timeout = infinite hang = cascade waiting to happen.
- **Base the value on the dependency's p99**, not the average — too tight and you fail healthy slow-ish requests; too loose and you hold resources during an incident. A common approach: timeout slightly above the dependency's normal p99.
- **Deadline propagation:** pass a shrinking deadline down the call chain so the total request has a budget. If the overall request must finish in 500ms and A already spent 300ms, A should give B at most ~200ms, not B's own 1s timeout. Without this, nested timeouts can exceed the client's patience (the client already gave up).
- **Fail fast, then recover:** on timeout, release the resource, return an error or a fallback (degraded response), and let a circuit breaker stop hammering a dead dependency. See [circuit-breakers-and-bulkheads](./circuit-breakers-and-bulkheads.md).

---

## E. Example — DMS calling the notification service

The DMS calls the notification service after processing a document. It sets a timeout of ~200ms (just above the notification service's p99 of ~120ms). If the notification service is slow/down, the DMS times out at 200ms, logs it, and **does not fail the upload** — notification is non-critical, so it degrades (the notification is retried async later). Without the timeout, a notification-service outage would hang DMS request threads, exhaust the pool, and take down *uploads* — a critical path — because of a *non-critical* dependency. The timeout + async fallback contains the blast radius. (Even better: make the notification call fully async via a queue — see [synchronous-vs-asynchronous-processing](../02-compute-and-architecture/synchronous-vs-asynchronous-processing.md).)

---

## F. Trade-offs

| Decision | Advantages | Disadvantages | Best use | Warning signs |
|---|---|---|---|---|
| No timeout | "Simplest" | Infinite hang → cascade | Never | Threads stuck on a dead dependency |
| Too-tight timeout | Fails fast | Fails healthy slow requests (false failures) | — | High error rate on a healthy dependency |
| p99-based timeout | Balances both | Needs tuning per dependency | **Default** | — |
| Deadline propagation | Bounds total request time | More plumbing | Deep call chains | Nested timeouts exceeding client patience |

---

## G. Failure scenarios

1. **Cascading overload (no/long timeout).** Trigger: a dependency slows; callers hang; pools exhaust up the chain. Users: total outage from one slow leaf. Detect: rising in-flight requests + latency; thread/connection pool saturation. Fix: timeouts + circuit breaker + bulkheads. Prevent: timeout every call, isolate dependencies.
2. **Timeout but the work succeeded.** Trigger: B finished just after A gave up. Effect: A thinks it failed and retries → duplicate. Fix: idempotency so the retry is harmless. See [retries-and-duplicate-execution](./retries-and-duplicate-execution.md).
3. **Timeout too tight → false failures.** Trigger: timeout below the dependency's real p99. Users: errors on a healthy system. Fix: raise to just above p99; investigate why p99 is high.
4. **Non-critical dependency fails the critical path.** Trigger: a critical operation synchronously waits on a non-critical dependency with no fallback. Fix: short timeout + degrade/fallback, or make it async.

---

## H. Scaling implications

Partial failure and the slow-vs-dead problem get *more* likely at scale (more services, more calls, more chances one is slow), and cascades are more damaging (more traffic to amplify). Timeouts, deadline propagation, circuit breakers, and bulkheads are the standard defenses — they convert "one slow dependency takes everything down" into "one slow dependency is contained and degraded." This is why these appear in every mature design and why reducing synchronous dependencies on the critical path (via async/queues) improves resilience. The blast-radius mindset — "what fails if this dependency is slow?" — is core SDE-2 reasoning.

---

## I. Interview preparation

**Basic**
- Q: Why set timeouts? → *A: You can't tell a slow dependency from a dead one; without a timeout, waiting threads pile up and a slow dependency cascades into a system-wide outage.*

**Intermediate**
- Q: How do you choose a timeout value? → *A: Base it on the dependency's p99 (slightly above), not the average — too tight fails healthy requests, too loose holds resources during incidents. Propagate a shrinking deadline down the call chain.*
- Q: What is deadline propagation? → *A: Passing a remaining-time budget down nested calls so the total request respects the client's deadline, instead of each hop using its own long timeout.*

**SDE-2 / scenario**
- Q: "One slow downstream service took down our whole API. Why and fix?" → *A: Missing/too-long timeouts let threads hang on the slow dependency; via Little's Law the pool exhausted and all requests failed (cascade). Fix: timeouts tuned to p99, a circuit breaker to fail fast, bulkheads to isolate that dependency's resources, and ideally make the call async.*
- Q: "A non-critical notification dependency keeps failing uploads." → *A: The critical path is synchronously coupled to a non-critical dependency. Short timeout + degrade (skip/retry async), or move the notification off the request path entirely via a queue.*

**Common wrong answers:** no timeouts; one global timeout; timeouts longer than the client's patience; coupling critical paths to non-critical dependencies with no fallback.

---

## J. Hands-on exercise

A request flows client → API (budget 800ms) → service A → service B → DB. Assign timeouts with deadline propagation, given B's p99 is 150ms and the DB's is 20ms. Explain what happens if B hangs with no timeout, and how a circuit breaker + bulkhead would contain it.

---

## K. Revision summary

**Key takeaways**
- Partial failure + can't-tell-slow-from-dead is the core problem; timeouts are the primary coping tool.
- No/long timeouts cause cascading overload (Little's Law): one slow leaf exhausts pools up the chain.
- Set timeouts from the dependency's p99; propagate deadlines down the chain; fail fast + degrade.
- A timeout can be wrong (work succeeded) → retries need idempotency; isolate critical paths from non-critical dependencies.

**Must explain without notes:** the cascade mechanism; p99-based timeouts; deadline propagation; the slow-vs-dead ambiguity.

**Rapid-fire**
1. Base a timeout on? → The dependency's p99 (slightly above).
2. What does a missing timeout cause? → Hang → pool exhaustion → cascade.
3. What is deadline propagation? → Passing a shrinking time budget down the call chain.
4. Timeout fired but work succeeded → risk? → Duplicate on retry (need idempotency).
5. Non-critical dependency on the critical path — fix? → Short timeout + degrade, or make it async.

**Next:** [retries-and-duplicate-execution](./retries-and-duplicate-execution.md)
