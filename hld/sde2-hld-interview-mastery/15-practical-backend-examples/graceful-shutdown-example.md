# Graceful Shutdown (Java / Spring Boot)

> Concept: [graceful-shutdown-and-draining](../07-reliability-and-resilience/graceful-shutdown-and-draining.md) · [deployment-strategies](../09-observability-and-operations/deployment-strategies.md)

**HLD point:** on SIGTERM (deploy/scale-in), stop taking new work, finish in-flight work (drain), then exit — so deploys/scaling drop no requests and lose no jobs. Idempotency still covers abrupt kills.

**Where the concern lives:** app (Spring lifecycle config) + platform (sends SIGTERM, waits a grace period).

```yaml
# application.yml — enable graceful shutdown + a grace period
server:
  shutdown: graceful                 # stop accepting new HTTP requests, finish in-flight ones
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # grace period (> your slowest request/job)
```

```java
// Readiness: fail readiness on shutdown so the LB stops routing NEW requests first.
@Component
class ShutdownReadiness {
    private volatile boolean shuttingDown = false;

    @EventListener(ContextClosedEvent.class)
    void onShutdown() { shuttingDown = true; }

    // /actuator/health/readiness reflects this -> LB drains this instance.
    public Status readiness() { return shuttingDown ? Status.OUT_OF_SERVICE : Status.UP; }
}
```

```java
// A message consumer: stop pulling new messages, finish + ack the current one, then exit.
@Component
class EmailConsumer implements SmartLifecycle {
    private volatile boolean running = true;

    @Override public void stop() {
        running = false;              // stop accepting new messages
        awaitInFlightCompletion();    // finish + ack the message being processed
    }
    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; } // shut down early among phases
}
```

**The shutdown sequence:** SIGTERM → fail readiness (LB stops new requests) → drain in-flight HTTP requests + finish/ack the current message → close DB/cache connections, flush metrics/logs → exit **within the grace period** (or the platform SIGKILLs).

**Key points to say:**
- Graceful shutdown makes **deploys and scale-in invisible** — no dropped requests, no lost in-flight jobs (prerequisite for zero-downtime rolling/canary deploys; see [deployment-strategies](../09-observability-and-operations/deployment-strategies.md)).
- **Fail readiness first** so the LB drains the instance before you stop processing (see [reverse-proxy-and-load-balancers](../02-compute-and-architecture/reverse-proxy-and-load-balancers.md)).
- Set the **grace period longer than your slowest request/job**, or work gets killed anyway.
- Graceful shutdown is **best-effort** — an abrupt kill (SIGKILL/OOM/crash) bypasses it, so a message consumer still needs **idempotency** (redelivery must be safe); graceful shutdown reduces disruption, idempotency guarantees correctness (see [idempotency-key-example](./idempotency-key-example.md)).

**Caveat:** production needs the grace period tuned to real request/job durations, readiness wired to the LB, and idempotent consumers for the abrupt-kill case.

**Maps to your resume:** your CI/CD + PM2 deploys — graceful shutdown is what turns a deploy from a brief error blip into a zero-downtime rollout (and PM2 similarly supports graceful reload).
