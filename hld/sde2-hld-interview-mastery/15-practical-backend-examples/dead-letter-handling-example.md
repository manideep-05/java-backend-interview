# Dead-Letter Handling (Java / Spring Boot + RabbitMQ)

> Concept: [dead-letter-queues](../05-messaging-and-event-driven-systems/dead-letter-queues.md)

**HLD point:** after N failed attempts, route a message to a **DLQ** so a poison message doesn't block the pipeline (head-of-line blocking); alert on DLQ growth; replay after a fix (to idempotent consumers).

**Where the concern lives:** the **broker** does the dead-lettering (dead-letter exchange); the app sets the retry limit, alerts, and replays.

```java
// Topology: main queue dead-letters to a DLX -> DLQ. (RabbitMQ)
@Configuration
class DlqConfig {
    @Bean DirectExchange dlx() { return new DirectExchange("notify.dlx", true, false); }
    @Bean Queue dlq() { return QueueBuilder.durable("notify.email.dlq").build(); }
    @Bean Binding dlqBind() { return BindingBuilder.bind(dlq()).to(dlx()).with("notify.email"); }

    @Bean Queue emailQueue() {
        return QueueBuilder.durable("notify.email")
            .withArgument("x-dead-letter-exchange", "notify.dlx")   // on reject/expire -> DLX
            .withArgument("x-dead-letter-routing-key", "notify.email")
            .build();
    }
}
```

```java
// Retry a few times, then dead-letter (basicNack with requeue=false -> goes to DLX).
@RabbitListener(queues = "notify.email", ackMode = "MANUAL")
public void onMessage(NotificationEvent evt, Channel ch,
                      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                      @Header(name = "x-death", required = false) List<Map<String,?>> xDeath)
        throws IOException {
    int attempts = deliveryCount(xDeath); // RabbitMQ tracks redelivery count in x-death
    try {
        process(evt);                              // idempotent
        ch.basicAck(tag, false);
    } catch (Exception e) {
        if (attempts >= 5) {
            ch.basicNack(tag, false, false);       // give up -> DLX -> DLQ (don't requeue)
            metrics.increment("dlq.routed");       // alert on this metric
        } else {
            ch.basicNack(tag, false, true);        // requeue to retry (or use a delay/retry queue)
        }
    }
}
```

```java
// Replay: move DLQ messages back to the main queue after fixing the root cause.
// Consumers are idempotent, so replay is safe (no double effect).
@Service
class DlqReplayer {
    void replay() {
        rabbit.receiveAndReply("notify.email.dlq", msg -> {
            republishToMain(msg);   // back to notify.exchange
            return null;            // ack the DLQ message
        });
    }
}
```

**Key points to say:**
- A **poison message** (bad payload, deleted reference) always fails → without a DLQ it's retried forever and, in an ordered queue, blocks everything behind it (**head-of-line blocking**).
- Cap retries, then **dead-letter + alert** (on DLQ depth/rate) — a silent, unmonitored DLQ loses work.
- **Replay requires idempotent consumers** (dedup on event id) so re-processing is safe (see [idempotency-key-example](./idempotency-key-example.md)).
- Tune the retry budget to the expected *transient*-failure duration — too low and you dead-letter recoverable messages.
- A proper delayed-retry (not immediate requeue) uses a retry queue with TTL + backoff (avoids tight requeue loops).

**Caveat:** production needs a delayed-retry mechanism (not naive requeue), DLQ depth alerting, replay tooling, and DLQ retention.

**Maps directly to your resume:** your notification platform "routes failures to a dead-letter queue" — this is the Spring/RabbitMQ expression. Add "and I alert on DLQ growth and can replay after a fix because consumers are idempotent."
