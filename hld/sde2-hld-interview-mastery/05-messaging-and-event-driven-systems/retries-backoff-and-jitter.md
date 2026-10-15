# Retries, Exponential Backoff, and Jitter

> Prerequisites: [message-delivery-semantics](./message-delivery-semantics.md)
> Related: [dead-letter-queues](./dead-letter-queues.md) · [idempotency-and-deduplication](./idempotency-and-deduplication.md) · [06-distributed-systems/circuit-breakers-and-bulkheads](../06-distributed-systems/circuit-breakers-and-bulkheads.md)

Retries are essential for resilience and dangerous when naive. The two must-know ideas: **exponential backoff** (don't hammer a struggling dependency) and **jitter** (don't synchronize retries into a thundering herd). Plus: only retry what's safe to retry.

---

## A. Problem and motivation

Transient failures (a brief network blip, a momentarily overloaded service) are common in distributed systems; retrying usually succeeds. But naive retries cause two disasters: (1) retrying immediately and repeatedly hammers an already-struggling dependency, turning a blip into an outage (**retry storm**); (2) many clients retrying on the same schedule synchronize into coordinated spikes (**thundering herd**). And retrying non-idempotent operations duplicates side effects. Done right, retries add resilience; done wrong, they amplify failures.

---

## B. Exponential backoff

Instead of retrying immediately and constantly, wait progressively longer between attempts: `delay = base × 2^attempt` (e.g., 1s, 2s, 4s, 8s…), capped at a max. This gives the struggling dependency time to recover and reduces load during the incident.

```mermaid
flowchart LR
    F[Request fails] --> R1[Wait 1s -> retry]
    R1 --> R2[Wait 2s -> retry]
    R2 --> R3[Wait 4s -> retry]
    R3 --> R4[Wait 8s -> retry]
    R4 --> C[Give up -> DLQ / error]
```

**Why not fixed-interval retries?** Fixed short intervals keep pounding a dead service (no recovery room) and can sustain an overload. Exponential backoff backs off fast, letting the system breathe.

---

## C. Jitter (the piece people forget)

Exponential backoff alone has a flaw: if 1,000 clients all fail at the same instant (e.g., a dependency hiccup), they all back off *on the same schedule* and retry at exactly 1s, 2s, 4s together — synchronized spikes that re-overload the dependency (**thundering herd**). **Jitter** adds randomness to the delay so retries spread out:

- **Full jitter:** `delay = random(0, base × 2^attempt)` — spreads retries across the whole window.
- **Equal/decorrelated jitter:** variants that balance spread and promptness.

```mermaid
flowchart TD
    NB[No jitter: all retry at 1s, 2s, 4s together -> spikes]
    WB[With jitter: retries spread randomly across the window -> smooth]
```

**Interview-ready:** "I use exponential backoff *with jitter* — backoff gives the dependency recovery time, and jitter prevents synchronized retry storms. Without jitter, backoff alone can create coordinated spikes."

---

## D. What's safe to retry (idempotency gate)

**Only retry idempotent operations**, or operations protected by an idempotency key. Retrying a non-idempotent POST (charge card, create order) can duplicate the effect — a retry after a timeout where the original actually succeeded causes a double charge. See [message-delivery-semantics](./message-delivery-semantics.md) and [idempotency-and-deduplication](./idempotency-and-deduplication.md).

Also distinguish **retriable vs non-retriable errors**: retry on 5xx/timeouts/connection errors (likely transient); do **not** retry on 4xx like 400/401/403/404 (the request is wrong; retrying won't help and wastes capacity). Retrying a 400 forever is a common bug.

**Retry budget / max attempts:** cap retries (e.g., 5) and the total time, then route to a DLQ or surface an error. Unbounded retries = infinite loops and resource exhaustion. Combine with a **circuit breaker** so you stop retrying a dependency that's down entirely. See [circuit-breakers-and-bulkheads](../06-distributed-systems/circuit-breakers-and-bulkheads.md).

---

## E. Example — notification service / webhooks

A webhook delivery or email send fails (receiver 503). The delivery worker retries with exponential backoff + full jitter: ~1s, ~2s, ~4s… up to a max window (e.g., several hours for webhooks). It retries only on 5xx/timeout, not on a 400 (bad payload). After N attempts it routes to a [DLQ](./dead-letter-queues.md) and alerts. A circuit breaker trips if a receiver is consistently down, so workers stop wasting attempts on it and aren't tied up. Because delivery is at-least-once and retried, the receiver dedupes on `event_id`. See [webhooks-and-callbacks](../01-networking-and-api-design/webhooks-and-callbacks.md).

---

## F. Trade-offs

| Technique | Advantages | Disadvantages | Best use | Warning signs |
|---|---|---|---|---|
| Immediate/fixed retry | Simple | Hammers struggling dependency; retry storms | Almost never at scale | Sustained overload during incidents |
| Exponential backoff | Gives recovery room | Alone → synchronized spikes | Transient failures | No jitter |
| + Jitter | Spreads retries, no herd | Slightly less predictable timing | **Default** | — |
| Retry budget + circuit breaker | Bounds damage, fails fast | Needs tuning | Protecting dependencies | Unbounded retries |

---

## G. Failure scenarios

1. **Retry storm.** Trigger: fixed/immediate retries against a struggling dependency. Effect: added load keeps it down (a blip becomes an outage). Fix: exponential backoff + jitter + circuit breaker. 
2. **Thundering herd.** Trigger: many clients fail together, back off on the same schedule. Effect: synchronized retry spikes. Fix: jitter. 
3. **Duplicate effect from retrying non-idempotent op.** Trigger: retrying a charge after a timeout where it actually succeeded. Users: double charge. Fix: idempotency keys; only retry idempotent ops.
4. **Infinite retry of a 4xx.** Trigger: retrying a permanently-bad request. Effect: wasted capacity, stuck message. Fix: don't retry non-retriable errors; cap attempts → DLQ.

---

## H. Scaling implications

At scale, retries can become a dominant and dangerous source of load — a dependency hiccup can trigger a self-amplifying retry storm across thousands of clients. Backoff + jitter + bounded retries + circuit breakers are what keep retries from turning transient faults into outages. The retry policy interacts with idempotency (safe to retry only if idempotent), DLQs (where exhausted messages go), and backpressure (retries add load that must be absorbed). A mature design states its retry policy explicitly: which errors, backoff formula, jitter, max attempts, and the DLQ/circuit-breaker interplay.

---

## I. Interview preparation

**Basic**
- Q: Why exponential backoff? → *A: Progressively longer waits give a struggling dependency time to recover and reduce load, instead of hammering it with immediate retries.*
- Q: Why jitter? → *A: Without it, many clients retrying on the same backoff schedule synchronize into spikes (thundering herd); jitter randomizes delays to spread them out.*

**Intermediate**
- Q: What should you not retry? → *A: Non-idempotent operations (without an idempotency key) and non-retriable errors (4xx like 400/401/404). Retry 5xx/timeouts/connection errors.*
- Q: How do retries and circuit breakers combine? → *A: Backoff+jitter handle transient faults; a circuit breaker stops retrying entirely when a dependency is clearly down, failing fast and freeing resources.*

**SDE-2 / scenario**
- Q: "A downstream hiccup caused a full outage — why?" → *A: Likely a retry storm: immediate/fixed retries from many clients piled load onto the struggling service, keeping it down. Fix with exponential backoff + jitter, bounded retries, and a circuit breaker.*
- Q: "Is it safe to retry a payment charge on timeout?" → *A: Only with an idempotency key — the original may have succeeded, so a blind retry double-charges. Attach an idempotency key so the retry is deduped.*

**Common wrong answers:** immediate/unbounded retries; backoff without jitter; retrying non-idempotent ops or 4xx errors; no DLQ/circuit breaker.

---

## J. Hands-on exercise

Design the retry policy for a worker calling a flaky third-party API: which errors to retry, the backoff formula with jitter, max attempts and total time budget, what happens on exhaustion, and how a circuit breaker fits. Note which operations need an idempotency key before retrying.

---

## K. Revision summary

**Key takeaways**
- Exponential backoff: progressively longer waits give dependencies recovery room and cut load.
- Jitter: randomize delays to prevent synchronized retry storms (thundering herd).
- Retry only idempotent ops (or with an idempotency key) and only retriable errors (5xx/timeouts), not 4xx.
- Bound retries (budget + max time) → DLQ; pair with a circuit breaker to fail fast.

**Must explain without notes:** backoff + why; jitter + why; what's safe to retry; retry budget + circuit breaker.

**Rapid-fire**
1. Formula for exponential backoff? → base × 2^attempt (capped).
2. What does jitter prevent? → Thundering herd / synchronized retries.
3. Retry a 404? → No (non-retriable).
4. Safe to retry a non-idempotent op? → Only with an idempotency key.
5. Where do exhausted retries go? → DLQ (+ alert).

**Next:** [dead-letter-queues](./dead-letter-queues.md)
