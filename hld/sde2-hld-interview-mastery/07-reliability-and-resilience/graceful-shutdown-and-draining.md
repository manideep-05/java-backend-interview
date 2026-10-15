# Graceful Shutdown and Connection Draining

> Prerequisites: [02-compute-and-architecture/reverse-proxy-and-load-balancers](../02-compute-and-architecture/reverse-proxy-and-load-balancers.md)
> Related: [09-observability-and-operations/deployment-strategies](../09-observability-and-operations/deployment-strategies.md) · [05-messaging/backpressure-and-consumer-lag](../05-messaging-and-event-driven-systems/backpressure-and-consumer-lag.md)

Instances are stopped constantly — deploys, autoscaling, failures. Stopping them *gracefully* (finishing in-flight work, not dropping requests) is what makes deploys and scaling invisible to users.

---

## A. Problem and motivation

Every deploy and scale-in event stops instances. If an instance is killed abruptly while serving requests, those requests fail (users see 5xx), in-flight jobs are lost, and messages may be left unacked (redelivered). Graceful shutdown ensures an instance finishes its current work and stops accepting new work *before* it dies, so stopping it is invisible. Without it, every deploy causes a blip and every scale-in drops work.

---

## B. The graceful shutdown sequence

```mermaid
sequenceDiagram
    participant Orch as Orchestrator/LB
    participant App as Instance
    Orch->>App: SIGTERM (please stop)
    App->>App: 1. Fail readiness check -> LB stops sending NEW requests
    App->>App: 2. Finish in-flight requests (drain)
    App->>App: 3. Stop consuming new queue messages; finish/ack current
    App->>App: 4. Close connections, flush, exit
    Note over Orch,App: SIGKILL only if it exceeds the grace period
```

1. **Signal:** the orchestrator sends SIGTERM (not an immediate kill), giving a grace period.
2. **Stop taking new work:** fail the readiness check so the LB removes this instance from rotation (no new HTTP requests); stop pulling new queue messages.
3. **Drain in-flight work:** let current requests finish and current messages get processed and acked (connection draining).
4. **Clean up:** close DB/cache connections, flush buffers (metrics, logs), then exit — before the grace period expires (or the orchestrator SIGKILLs it).

The LB's health checks + draining (from [reverse-proxy-and-load-balancers](../02-compute-and-architecture/reverse-proxy-and-load-balancers.md)) are what make this work for HTTP; for workers, it's "stop consuming, finish current, ack."

---

## C. The idempotency safety net

Even with graceful shutdown, abrupt failures happen (SIGKILL, crash, OOM). A worker killed mid-message leaves it unacked → redelivered → processed again. So graceful shutdown reduces disruption but **idempotency is still required** for correctness (see [idempotency-and-deduplication](../05-messaging-and-event-driven-systems/idempotency-and-deduplication.md)). Graceful shutdown is best-effort cleanliness; idempotency is the guarantee.

---

## D. Example — DMS

- **App instance on deploy:** receives SIGTERM → fails readiness (LB stops routing new requests) → finishes the ~handful of in-flight requests → closes connections → exits within the grace period. Users see no errors (rolling deploy, see [deployment-strategies](../09-observability-and-operations/deployment-strategies.md)).
- **OCR worker on scale-in:** stops pulling new jobs → finishes the current job and acks it → exits. If instead it were killed abruptly mid-job, the job (unacked) is redelivered and reprocessed — harmless because OCR is idempotent (keyed by document+revision).

---

## E. Trade-offs / failure scenarios

| Scenario | Without graceful shutdown | With it |
|---|---|---|
| Deploy | In-flight requests dropped (5xx blip) | Requests finish; no blip |
| Scale-in | In-flight jobs lost / messages redelivered | Current work finishes + acks |
| Grace period too short | Work killed before finishing | Set grace > max request/job duration |
| Abrupt crash (SIGKILL/OOM) | Work lost | Mitigated by idempotency (redelivery is safe) |

Failure modes: grace period shorter than the longest request/job (work killed anyway — size it right); not failing readiness first (LB keeps sending new requests during drain); not stopping queue consumption (new messages pulled during shutdown).

---

## F. Scaling implications

At scale, instances churn constantly (continuous deployment, aggressive autoscaling), so graceful shutdown is what keeps that churn invisible — without it, every deploy and scale event causes user-visible errors and lost work, which at high frequency is a serious reliability drag. It's a prerequisite for zero-downtime deploys and clean autoscaling. Combined with idempotency (for the abrupt-failure cases it can't cover), it makes instance lifecycle events safe.

---

## G. Interview preparation

**Basic**
- Q: What is graceful shutdown? → *A: On stop, an instance stops taking new work, finishes in-flight work (drains), cleans up, then exits — so stopping it doesn't drop requests or lose jobs.*

**Intermediate**
- Q: What's the sequence? → *A: SIGTERM → fail readiness (LB stops new requests) → drain in-flight requests / finish+ack current messages → close connections, flush → exit within the grace period.*
- Q: Why is idempotency still needed with graceful shutdown? → *A: Abrupt failures (SIGKILL/crash/OOM) still happen; a worker killed mid-message causes redelivery → idempotency makes the reprocessing safe. Graceful shutdown is best-effort; idempotency is the guarantee.*

**SDE-2 / scenario**
- Q: "Deploys cause brief 5xx spikes." → *A: Instances are being killed with in-flight requests. Implement graceful shutdown: on SIGTERM, fail readiness so the LB drains the instance, finish in-flight requests, then exit — within a grace period longer than the slowest request.*
- Q: "Scale-in loses jobs." → *A: Workers are killed mid-job. On SIGTERM, stop consuming new messages, finish and ack the current one, then exit; and ensure idempotency so any abrupt kill's redelivery is safe.*

**Common wrong answers:** abrupt kill on deploy; grace period shorter than the longest request; not failing readiness first; relying on graceful shutdown alone (no idempotency) for correctness.

---

## H. Hands-on exercise

Define the graceful-shutdown behavior for (a) an HTTP app instance and (b) a queue worker, including the signal handled, the drain steps, the grace-period sizing, and why idempotency is still required for the worker.

---

## I. Revision summary

**Key takeaways**
- Graceful shutdown: stop new work (fail readiness / stop consuming) → drain in-flight → clean up → exit within the grace period.
- Makes deploys/scaling invisible (no dropped requests/lost jobs).
- Idempotency is still required — abrupt failures bypass graceful shutdown; redelivery must be safe.
- Prerequisite for zero-downtime deploys + clean autoscaling.

**Rapid-fire**
1. First step on SIGTERM? → Fail readiness so the LB stops new requests.
2. What does "drain" mean? → Finish in-flight requests before exiting.
3. Grace period must exceed? → The slowest request/job duration.
4. Why still need idempotency? → Abrupt kills (SIGKILL/crash) → redelivery.
5. What does graceful shutdown enable? → Zero-downtime deploys + clean scale-in.

**Next:** [disaster-recovery](./disaster-recovery.md)
